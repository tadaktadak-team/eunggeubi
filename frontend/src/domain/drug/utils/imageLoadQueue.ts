// 식약처 이미지 서버(nedrug.mfds.go.kr)는 IP당 요청 한도가 있어서(실측: 처음 약 20장 정도만 받고
// 그 뒤는 분당 약 15장, 넘으면 429로 거절) 짧은 시간에 요청이 몰리면 이미지가 뜨지 않는다. 거절당하는
// 중에도 계속 요청하면 차단이 풀리지 않는다. 또 응답이 cache-control: no-store라 같은 이미지를 다시
// 그릴 때도 서버에서 다시 받는다. 그래서 클라이언트에서 이미지 요청 속도를 직접 조절한다.
//
// 방식은 토큰 버킷 + 정지(cooldown)이다.
// - 처음에는 capacity장(기본 15장)을 바로 받고, 그 뒤에는 refillIntervalMs마다 1장씩(기본 4초 =
//   분당 15장) 허용한다.
// - 이미지를 그릴 때마다 반드시 대기열을 거친다. 예전에는 "기기 캐시에 있다"는 답(queryCache)이나
//   "한 번 허가받은 URL"이면 토큰 없이 통과시켰는데, no-store라 실제로는 서버에 다시 요청이 나가서
//   한도를 넘기는 우회 경로가 됐다(팀 측정: 실패 136회 중 114회가 이 경로).
// - 꺼내는 순서는 상세 사진(high) → 목록 위쪽 행(order가 작은 것) → 먼저 요청한 것 순이다.
//   화면이 위에서 아래로 차례로 채워지게 하기 위해서다.
// - 이미지 한 장의 실패는 그 이미지만 다시 시도한다(깨진 주소 하나 때문에 전체를 멈추지 않음).
//   서버가 거절할 때는 여러 장이 연달아 실패하므로, rejectionWindowMs(기본 10초) 안에 서로 다른
//   이미지가 rejectionMinFailures장(기본 3장) 이상 실패하면 거절로 보고 cooldownMs(기본 15초) 동안
//   모든 요청을 멈춘다. 쉬고 난 뒤에는 토큰 0에서 천천히(4초에 1장) 다시 받는다.
//
// 이 파일은 react-native에 의존하지 않는 순수 로직이라 가짜 시계로 단위 검증할 수 있다.

export type SlotPriority = 'normal' | 'high';

export interface AcquireOptions {
  priority?: SlotPriority;
  order?: number; // 목록에서의 행 번호. 작을수록 먼저 받는다(위쪽 행부터).
}

interface Entry {
  uri: string;
  priority: SlotPriority;
  order: number;
  seq: number; // order가 같으면 먼저 요청한 것부터
  listeners: Set<() => void>;
}

export type ImageQueueEvent =
  | { type: 'cooldown-start'; distinctFailures: number; totalFailures: number; totalGranted: number }
  | { type: 'cooldown-end'; totalFailures: number; totalGranted: number };

export interface ImageLoadQueueOptions {
  capacity: number;              // 처음에 바로 받을 수 있는 장수(버킷 크기)
  refillIntervalMs: number;      // 이후 1장을 더 허용하는 간격
  cooldownMs: number;            // 거절로 판단했을 때 모든 요청을 멈추는 시간
  rejectionWindowMs: number;     // 이 시간 안에
  rejectionMinFailures: number;  // 서로 다른 이미지가 이만큼 실패하면 거절로 판단
  retryDelayMs: number;          // 한 장만 실패했을 때 그 이미지를 다시 시도하기까지 기다리는 시간
  maxRetries: number;            // 실패한 이미지를 다시 시도할 최대 횟수(이후엔 포기하고 아이콘 유지)
  now?: () => number;
  setTimer?: (fn: () => void, ms: number) => unknown;
  clearTimer?: (handle: unknown) => void;
  onEvent?: (event: ImageQueueEvent) => void;
}

export interface ImageLoadQueue {
  // uri를 로드해도 된다는 허가가 나면 onGranted를 호출한다. 반환값은 대기를 취소하는 함수.
  acquire: (uri: string, onGranted: () => void, options?: AcquireOptions) => () => void;
  // 이미지 로드 실패를 알린다. 이 URL의 누적 실패 횟수를 반환한다.
  reportFailure: (uri: string) => number;
  recordSuccess: (uri: string) => void;
  hasGivenUp: (uri: string) => boolean;
  isCoolingDown: () => boolean;
  // 실패한 이미지를 다시 시도해도 되는 시점에 callback을 한 번 호출한다(정지 중이면 정지가 끝날 때,
  // 아니면 retryDelayMs 뒤). 반환값은 예약 취소 함수.
  scheduleRetry: (callback: () => void) => () => void;
  // 정지가 끝났을 때 한 번 호출된다(정지 중이 아니면 즉시 호출). 반환값은 등록 취소 함수.
  onResume: (callback: () => void) => () => void;
  debug: () => {
    tokens: number;
    pending: number;
    coolingDown: boolean;
    totalGranted: number;
    totalFailures: number;
    cooldowns: number;
  };
}

export function createImageLoadQueue(options: ImageLoadQueueOptions): ImageLoadQueue {
  const { capacity, refillIntervalMs, cooldownMs, rejectionWindowMs, rejectionMinFailures, retryDelayMs, maxRetries } =
    options;
  const now = options.now ?? Date.now;
  const setTimer = options.setTimer ?? ((fn: () => void, ms: number) => setTimeout(fn, ms));
  const clearTimer = options.clearTimer ?? ((h: unknown) => clearTimeout(h as ReturnType<typeof setTimeout>));
  const onEvent = options.onEvent ?? (() => {});
  const EPSILON = 1e-9;

  let tokens = capacity;
  let lastRefillAt = now();
  let cooldownUntil = 0;
  let timer: unknown = null;
  let seqCounter = 0;

  let totalGranted = 0;
  let totalFailures = 0;
  let cooldowns = 0;

  const failures = new Map<string, number>(); // URL별 누적 실패 횟수(재시도/포기 판단용)
  let recentFailures: { uri: string; at: number }[] = []; // 거절 판단용 최근 실패 기록
  const resumeCallbacks = new Set<() => void>();

  const pending: Entry[] = [];
  const pendingByUri = new Map<string, Entry>();

  const coolingDown = () => now() < cooldownUntil;

  const refill = () => {
    const t = now();
    // 정지 중에는 시간이 흘러도 토큰이 쌓이지 않는다(lastRefillAt이 정지 종료 시각이라 음수가 됨).
    tokens = Math.min(capacity, tokens + Math.max(0, t - lastRefillAt) / refillIntervalMs);
    lastRefillAt = Math.max(lastRefillAt, t);
  };

  const removePending = (entry: Entry) => {
    pendingByUri.delete(entry.uri);
    const i = pending.indexOf(entry);
    if (i >= 0) pending.splice(i, 1);
  };

  const grant = (entry: Entry) => {
    removePending(entry);
    totalGranted++;
    entry.listeners.forEach((listener) => listener());
  };

  // 다음에 허가할 항목: 위쪽 행(order가 작은 것) → 먼저 요청한 것(seq가 작은 것).
  const pickNext = (): Entry => {
    let best = pending[0];
    for (const entry of pending) {
      if (entry.order < best.order || (entry.order === best.order && entry.seq < best.seq)) best = entry;
    }
    return best;
  };

  const fireResume = () => {
    if (resumeCallbacks.size === 0) return;
    const callbacks = Array.from(resumeCallbacks);
    resumeCallbacks.clear();
    callbacks.forEach((callback) => callback());
  };

  const schedule = () => {
    if (timer !== null) return;
    const t = now();
    let wait: number;
    if (t < cooldownUntil) {
      wait = cooldownUntil - t;
    } else if (pending.length > 0) {
      wait = Math.max(0, Math.ceil((1 - tokens) * refillIntervalMs));
    } else {
      return;
    }
    timer = setTimer(() => {
      timer = null;
      if (!coolingDown() && cooldownUntil > 0 && cooldownUntil <= now()) {
        cooldownUntil = 0;
        onEvent({ type: 'cooldown-end', totalFailures, totalGranted });
        fireResume();
      }
      drain();
    }, wait);
  };

  function drain() {
    if (coolingDown()) {
      schedule();
      return;
    }
    refill();

    // 사용자가 직접 연 상세 사진(high)은 토큰이 없어도 먼저 내보낸다. 대신 토큰을 차감해서
    // (버킷이 비어 있으면 음수까지) 뒤따르는 썸네일이 그만큼 늦게 받도록 해 전체 속도는 지킨다.
    for (const entry of pending.filter((e) => e.priority === 'high')) {
      tokens = Math.max(tokens - 1, -capacity);
      grant(entry);
    }

    while (pending.length > 0 && tokens >= 1 - EPSILON) {
      tokens -= 1;
      grant(pickNext());
    }
    if (pending.length > 0) schedule();
  }

  const acquire = (uri: string, onGranted: () => void, acquireOptions: AcquireOptions = {}) => {
    const priority = acquireOptions.priority ?? 'normal';
    const order = acquireOptions.order ?? Number.MAX_SAFE_INTEGER;

    let entry = pendingByUri.get(uri);
    if (entry) {
      // 같은 URL이 이미 대기 중이면 요청을 합친다(토큰 1개로 함께 허가).
      if (priority === 'high') entry.priority = 'high';
      entry.order = Math.min(entry.order, order);
    } else {
      entry = { uri, priority, order, seq: ++seqCounter, listeners: new Set() };
      pendingByUri.set(uri, entry);
      pending.push(entry);
    }
    entry.listeners.add(onGranted);
    const myEntry = entry;

    drain();

    return () => {
      myEntry.listeners.delete(onGranted);
      if (myEntry.listeners.size === 0 && pendingByUri.get(uri) === myEntry) {
        removePending(myEntry);
        if (pending.length === 0 && resumeCallbacks.size === 0 && !coolingDown() && timer !== null) {
          clearTimer(timer);
          timer = null;
        }
      }
    };
  };

  const startCooldown = (distinctFailures: number) => {
    cooldownUntil = now() + cooldownMs;
    cooldowns++;
    // 쉬고 난 뒤에는 몰아서 받지 않고 토큰 0에서 천천히 다시 시작한다.
    tokens = 0;
    lastRefillAt = cooldownUntil;
    // 정지 전의 실패 기록은 비운다. 정지가 끝나자마자 예전 실패 때문에 다시 정지되지 않게 하기 위해서다.
    recentFailures = [];
    onEvent({ type: 'cooldown-start', distinctFailures, totalFailures, totalGranted });
    // 기존 대기 타이머는 정지 전 토큰 기준이라(상세 사진으로 토큰이 음수면 수십 초 뒤로 잡혀 있을 수 있음)
    // 그대로 두면 정지 종료가 15초보다 늦어진다. 지우고 정지 종료 시각에 맞춰 다시 잡는다.
    if (timer !== null) {
      clearTimer(timer);
      timer = null;
    }
    schedule();
  };

  const onResume = (callback: () => void) => {
    if (!coolingDown()) {
      callback();
      return () => {};
    }
    resumeCallbacks.add(callback);
    schedule();
    return () => {
      resumeCallbacks.delete(callback);
    };
  };

  return {
    acquire,
    reportFailure: (uri) => {
      const attempts = (failures.get(uri) ?? 0) + 1;
      failures.set(uri, attempts);
      totalFailures++;
      // 정지 중에 도착하는 실패는 정지 직전에 이미 나가 있던 요청의 결과일 뿐이라 집계하지 않는다
      // (집계하면 정지가 끝나자마자 다시 정지되거나 정지가 끝없이 길어질 수 있다).
      if (coolingDown()) return attempts;

      const t = now();
      recentFailures.push({ uri, at: t });
      recentFailures = recentFailures.filter((f) => t - f.at <= rejectionWindowMs);
      // 같은 이미지가 여러 번 실패한 건 그 이미지 주소 문제일 가능성이 커서, "서로 다른" 이미지 수로 판단한다.
      const distinct = new Set(recentFailures.map((f) => f.uri)).size;
      if (distinct >= rejectionMinFailures) startCooldown(distinct);
      return attempts;
    },
    recordSuccess: (uri) => {
      failures.delete(uri);
    },
    hasGivenUp: (uri) => (failures.get(uri) ?? 0) > maxRetries,
    isCoolingDown: coolingDown,
    scheduleRetry: (callback) => {
      if (coolingDown()) return onResume(callback);
      const handle = setTimer(callback, retryDelayMs);
      return () => clearTimer(handle);
    },
    onResume,
    debug: () => ({
      tokens,
      pending: pending.length,
      coolingDown: coolingDown(),
      totalGranted,
      totalFailures,
      cooldowns,
    }),
  };
}

// 앱 전체가 하나의 대기열을 공유한다(요청 제한은 화면이 아니라 기기/IP 단위이기 때문).
export const IMAGE_BURST_COUNT = 15;            // 서버 한도(약 20장)보다 여유 있게
export const IMAGE_REFILL_INTERVAL_MS = 4000;   // 분당 15장
export const IMAGE_COOLDOWN_MS = 15000;         // 거절로 판단했을 때 쉬는 시간
export const IMAGE_REJECTION_WINDOW_MS = 10000; // 10초 안에
export const IMAGE_REJECTION_MIN_FAILURES = 3;  // 서로 다른 이미지 3장 이상 실패하면 거절로 판단
export const IMAGE_RETRY_DELAY_MS = 10000;      // 한 장만 실패했을 때 재시도까지 기다리는 시간
export const IMAGE_MAX_RETRIES = 3;

export const imageLoadQueue = createImageLoadQueue({
  capacity: IMAGE_BURST_COUNT,
  refillIntervalMs: IMAGE_REFILL_INTERVAL_MS,
  cooldownMs: IMAGE_COOLDOWN_MS,
  rejectionWindowMs: IMAGE_REJECTION_WINDOW_MS,
  rejectionMinFailures: IMAGE_REJECTION_MIN_FAILURES,
  retryDelayMs: IMAGE_RETRY_DELAY_MS,
  maxRetries: IMAGE_MAX_RETRIES,
  // 개발 중에만 정지 시작/종료를 Metro 콘솔에 남긴다(팀에서 실패·정지 횟수를 다시 측정할 때 참고용).
  onEvent:
    process.env.NODE_ENV !== 'production'
      ? (event) => console.log('[imageLoadQueue]', JSON.stringify(event))
      : undefined,
});
