// 식약처 이미지 서버(nedrug.mfds.go.kr)는 짧은 시간에 요청이 몰리면 429(요청 제한)로 거절한다
// (실측: 동시 8개로 300장을 요청하자 약 100장 뒤부터 거절됨). 또 응답이 cache-control: no-store라
// 같은 이미지를 다시 봐도 다시 받아야 할 수 있다. 목록 한 번에 이미지가 수십 장씩 뜨는 화면이라
// 클라이언트에서 이미지 요청 속도를 직접 조절한다.
//
// 방식은 토큰 버킷이다: 처음에는 capacity장을 바로 받게 해주고, 그 뒤에는 refillIntervalMs마다
// 1장씩 허용한다(분당 15장 = 4초에 1장). 정확한 서버 한도는 모르므로 아래 기본값은 보수적인
// 추정값이고, 필요하면 상수만 바꿔서 조정한다.
//
// 이 파일은 react-native에 의존하지 않는 순수 로직이라 가짜 시계로 단위 검증할 수 있다.

export type SlotPriority = 'normal' | 'high';

interface Entry {
  uri: string;
  listeners: Set<() => void>;
}

export interface ImageLoadQueueOptions {
  capacity: number;          // 처음에 바로 받을 수 있는 장수(버킷 크기)
  refillIntervalMs: number;  // 이후 1장을 더 허용하는 간격
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
  recordFailure: (uri: string) => number;
  recordSuccess: (uri: string) => void;
  hasGivenUp: (uri: string) => boolean;
  debug: () => { tokens: number; pending: number; granted: number };
}

export function createImageLoadQueue(options: ImageLoadQueueOptions): ImageLoadQueue {
  const { capacity, refillIntervalMs, maxRetries } = options;
  const now = options.now ?? Date.now;
  const setTimer = options.setTimer ?? ((fn: () => void, ms: number) => setTimeout(fn, ms));
  const clearTimer = options.clearTimer ?? ((h: unknown) => clearTimeout(h as ReturnType<typeof setTimeout>));
  const EPSILON = 1e-9;

  let tokens = capacity;
  let lastRefillAt = now();
  let timer: unknown = null;

  // 이번 실행 중 이미 허가를 받은 URL. 한 번 받은 이미지를 다시 보여줄 때(스크롤로 되돌아오기 등)
  // 토큰을 또 쓰지 않게 한다.
  const granted = new Set<string>();
  const failures = new Map<string, number>();

  // 대기열: 뒤쪽이 가장 최근에 화면에 보인 이미지다. 사용자가 지금 보고 있는 것부터 받도록
  // 뒤에서부터 꺼낸다(LIFO).
  const pending: Entry[] = [];
  const pendingByUri = new Map<string, Entry>();

  const refill = () => {
    const t = now();
    tokens = Math.min(capacity, tokens + (t - lastRefillAt) / refillIntervalMs);
    lastRefillAt = t;
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
    const wait = Math.max(0, Math.ceil((1 - tokens) * refillIntervalMs));
    timer = setTimer(() => {
      timer = null;
      drain();
    }, wait);
  };

  function drain() {
    refill();
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

    if (priority === 'high') {
      // 사용자가 직접 연 상세 화면의 사진 한 장은 기다리게 하지 않는다. 대신 토큰은 차감해서
      // (버킷이 비어 있으면 음수까지) 뒤따르는 썸네일이 그만큼 늦게 받도록 한다.
      refill();
      tokens = Math.max(tokens - 1, -capacity);
      const waiting = pendingByUri.get(uri);
      if (waiting) {
        removePending(waiting);
        grant(waiting);
      }
      granted.add(uri);
      onGranted();
      return () => {};
    }

    let entry = pendingByUri.get(uri);
    if (entry) {
      // 같은 URL이 이미 대기 중이면 요청을 합치고, 다시 화면에 보였으니 가장 최근 순위로 올린다.
      const i = pending.indexOf(entry);
      if (i >= 0) pending.splice(i, 1);
      pending.push(entry);
    } else {
      entry = { uri, listeners: new Set() };
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
        if (pending.length === 0 && timer !== null) {
          clearTimer(timer);
          timer = null;
        }
      }
    };
  };

  return {
    acquire,
    markGranted: (uri) => {
      granted.add(uri);
    },
    forgetGranted: (uri) => {
      granted.delete(uri);
    },
    recordFailure: (uri) => {
      const n = (failures.get(uri) ?? 0) + 1;
      failures.set(uri, n);
      return n;
    },
    recordSuccess: (uri) => {
      failures.delete(uri);
    },
    hasGivenUp: (uri) => (failures.get(uri) ?? 0) > maxRetries,
    debug: () => ({ tokens, pending: pending.length, granted: granted.size }),
  };
}

// 앱 전체가 하나의 대기열을 공유한다(요청 제한은 화면이 아니라 기기/IP 단위이기 때문).
export const IMAGE_BURST_COUNT = 20;
export const IMAGE_REFILL_INTERVAL_MS = 4000; // 분당 15장
export const IMAGE_MAX_RETRIES = 2;
export const IMAGE_RETRY_DELAY_MS = 60000;

export const imageLoadQueue = createImageLoadQueue({
  capacity: IMAGE_BURST_COUNT,
  refillIntervalMs: IMAGE_REFILL_INTERVAL_MS,
  maxRetries: IMAGE_MAX_RETRIES,
});
