// 식약처 이미지 서버(nedrug.mfds.go.kr)는 IP당 요청 한도가 있어서(실측: 처음 약 20장 정도만 받고
// 그 뒤는 429로 거절) 짧은 시간에 요청이 몰리면 이미지가 뜨지 않는다. 더 나쁜 점은 거절당하는
// 중에도 계속 요청하면 차단이 풀리지 않는다는 것이다. 또 응답이 cache-control: no-store라 같은
// 이미지를 다시 봐도 다시 받아야 할 수 있다. 목록 한 번에 이미지가 수십 장씩 뜨는 화면이라
// 클라이언트에서 이미지 요청 속도를 직접 조절한다.
//
// 방식은 토큰 버킷 + 정지(cooldown)이다.
// - 처음에는 capacity장(기본 15장, 한도 약 20장보다 여유 있게)을 바로 받게 해주고, 그 뒤에는
//   refillIntervalMs마다 1장씩(기본 4초 = 분당 15장) 허용한다.
// - 이미지가 하나라도 실패하면 거절당했을 가능성이 있다고 보고, 기기 전체에서 cooldownMs(기본 15초)
//   동안 새 요청을 멈춘다. 쉬는 동안에는 상세 사진을 포함해 어떤 요청도 보내지 않는다.
//   쉬고 난 뒤에는 토큰 0에서 시작해서 천천히(4초에 1장) 다시 받는다.
// - 실패한 이미지는 정지가 끝나면 대기열로 돌아와 순서대로 다시 받는다.
//
// 이 파일은 react-native에 의존하지 않는 순수 로직이라 가짜 시계로 단위 검증할 수 있다.

export type SlotPriority = 'normal' | 'high';

interface Entry {
  uri: string;
  priority: SlotPriority;
  listeners: Set<() => void>;
}

export interface ImageLoadQueueOptions {
  capacity: number;          // 처음에 바로 받을 수 있는 장수(버킷 크기)
  refillIntervalMs: number;  // 이후 1장을 더 허용하는 간격
  cooldownMs: number;        // 거절이 의심될 때 모든 요청을 멈추는 시간
  maxRetries: number;        // 실패한 이미지를 다시 시도할 최대 횟수(이후엔 포기하고 아이콘 유지)
  now?: () => number;
  setTimer?: (fn: () => void, ms: number) => unknown;
  clearTimer?: (handle: unknown) => void;
}

export interface ImageLoadQueue {
  // uri를 로드해도 된다는 허가가 나면 onGranted를 호출한다. 반환값은 대기를 취소하는 함수.
  acquire: (uri: string, onGranted: () => void, priority?: SlotPriority) => () => void;
  markGranted: (uri: string) => void;
  forgetGranted: (uri: string) => void;
  // 이미지 로드 실패를 알린다. 이 URL의 누적 실패 횟수를 반환한다.
  // 처음 실패한 URL이면 거절당한 것으로 보고 정지를 시작한다(반복 실패는 깨진 이미지일 가능성이
  // 커서 정지시키지 않고 재시도 횟수만 소모한다).
  reportFailure: (uri: string) => number;
  recordSuccess: (uri: string) => void;
  hasGivenUp: (uri: string) => boolean;
  isCoolingDown: () => boolean;
  // 정지가 끝났을 때 한 번 호출된다(정지 중이 아니면 즉시 호출). 반환값은 등록 취소 함수.
  onResume: (callback: () => void) => () => void;
  debug: () => { tokens: number; pending: number; granted: number; coolingDown: boolean };
}

export function createImageLoadQueue(options: ImageLoadQueueOptions): ImageLoadQueue {
  const { capacity, refillIntervalMs, cooldownMs, maxRetries } = options;
  const now = options.now ?? Date.now;
  const setTimer = options.setTimer ?? ((fn: () => void, ms: number) => setTimeout(fn, ms));
  const clearTimer = options.clearTimer ?? ((h: unknown) => clearTimeout(h as ReturnType<typeof setTimeout>));
  const EPSILON = 1e-9;

  let tokens = capacity;
  let lastRefillAt = now();
  let cooldownUntil = 0;
  let timer: unknown = null;

  // 이번 실행 중 이미 허가를 받은 URL. 한 번 받은 이미지를 다시 보여줄 때(스크롤로 되돌아오기 등)
  // 토큰을 또 쓰지 않게 한다.
  const granted = new Set<string>();
  const failures = new Map<string, number>();
  const resumeCallbacks = new Set<() => void>();

  // 대기열: 뒤쪽이 가장 최근에 화면에 보인 이미지다. 사용자가 지금 보고 있는 것부터 받도록
  // 뒤에서부터 꺼낸다(LIFO).
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
    granted.add(entry.uri);
    entry.listeners.forEach((listener) => listener());
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
      if (!coolingDown() && resumeCallbacks.size > 0) {
        const callbacks = Array.from(resumeCallbacks);
        resumeCallbacks.clear();
        callbacks.forEach((callback) => callback());
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
    // (버킷이 비어 있으면 음수까지) 뒤따르는 썸네일이 그만큼 늦게 받도록 한다.
    for (let i = pending.length - 1; i >= 0; i--) {
      const entry = pending[i];
      if (entry.priority !== 'high') continue;
      removePending(entry);
      tokens = Math.max(tokens - 1, -capacity);
      grant(entry);
    }

    while (pending.length > 0 && tokens >= 1 - EPSILON) {
      const entry = pending.pop() as Entry;
      pendingByUri.delete(entry.uri);
      tokens -= 1;
      grant(entry);
    }
    if (pending.length > 0) schedule();
  }

  const acquire = (uri: string, onGranted: () => void, priority: SlotPriority = 'normal') => {
    if (granted.has(uri)) {
      onGranted();
      return () => {};
    }

    let entry = pendingByUri.get(uri);
    if (entry) {
      // 같은 URL이 이미 대기 중이면 요청을 합치고, 다시 화면에 보였으니 가장 최근 순위로 올린다.
      const i = pending.indexOf(entry);
      if (i >= 0) pending.splice(i, 1);
      pending.push(entry);
      if (priority === 'high') entry.priority = 'high';
    } else {
      entry = { uri, priority, listeners: new Set() };
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

  const startCooldown = () => {
    // 이미 쉬는 중이면 연장하지 않는다. 정지 중에 도착하는 실패는 정지 직전에 이미 나가 있던
    // 요청의 결과일 뿐이라, 연장하면 정지가 끝없이 길어질 수 있다.
    if (coolingDown()) return;
    cooldownUntil = now() + cooldownMs;
    // 쉬고 난 뒤에는 몰아서 받지 않고 토큰 0에서 천천히 다시 시작한다.
    tokens = 0;
    lastRefillAt = cooldownUntil;
    schedule();
  };

  return {
    acquire,
    markGranted: (uri) => {
      granted.add(uri);
    },
    forgetGranted: (uri) => {
      granted.delete(uri);
    },
    reportFailure: (uri) => {
      const attempts = (failures.get(uri) ?? 0) + 1;
      failures.set(uri, attempts);
      if (attempts === 1) startCooldown();
      return attempts;
    },
    recordSuccess: (uri) => {
      failures.delete(uri);
    },
    hasGivenUp: (uri) => (failures.get(uri) ?? 0) > maxRetries,
    isCoolingDown: coolingDown,
    onResume: (callback) => {
      if (!coolingDown()) {
        callback();
        return () => {};
      }
      resumeCallbacks.add(callback);
      schedule();
      return () => {
        resumeCallbacks.delete(callback);
      };
    },
    debug: () => ({ tokens, pending: pending.length, granted: granted.size, coolingDown: coolingDown() }),
  };
}

// 앱 전체가 하나의 대기열을 공유한다(요청 제한은 화면이 아니라 기기/IP 단위이기 때문).
export const IMAGE_BURST_COUNT = 15;          // 서버 한도(약 20장)보다 여유 있게
export const IMAGE_REFILL_INTERVAL_MS = 4000; // 분당 15장
export const IMAGE_COOLDOWN_MS = 15000;       // 거절 의심 시 쉬는 시간
export const IMAGE_MAX_RETRIES = 3;           // 거절로 실패한 이미지도 횟수를 소모하므로 넉넉히

export const imageLoadQueue = createImageLoadQueue({
  capacity: IMAGE_BURST_COUNT,
  refillIntervalMs: IMAGE_REFILL_INTERVAL_MS,
  cooldownMs: IMAGE_COOLDOWN_MS,
  maxRetries: IMAGE_MAX_RETRIES,
});
