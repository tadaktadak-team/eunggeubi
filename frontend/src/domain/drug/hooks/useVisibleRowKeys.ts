import { useCallback, useState } from 'react';
import type { ViewToken } from 'react-native';

// FlatList는 화면 위아래로 여러 화면 분량의 행을 미리 그려 둔다(예: 낱알 검색은 windowSize=7이라
// 약 50행). 그 행들이 전부 약 이미지를 요청하면 서버 한도(분당 약 15장) 때문에 대기열이 수십 개씩
// 쌓이고, 정작 지금 보이는 행이 뒤로 밀린다. 그래서 "지금 화면에 보이는 행"만 이미지를 요청하도록
// 보이는 행의 key(keyExtractor 값)를 추적한다.

// "보인다"의 기준: 행의 30% 이상이 0.2초 이상 화면에 있을 때. 빠르게 훑고 지나가는 행까지 이미지를
// 요청하지 않도록 최소 노출 시간을 둔다. FlatList는 이 값이 렌더링마다 바뀌면 안 되므로 상수로 둔다.
export const VISIBLE_ROW_CONFIG = { itemVisiblePercentThreshold: 30, minimumViewTime: 200 };

function sameKeys(a: ReadonlySet<string>, b: ReadonlySet<string>): boolean {
  if (a.size !== b.size) return false;
  for (const key of a) if (!b.has(key)) return false;
  return true;
}

export function useVisibleRowKeys() {
  const [visibleKeys, setVisibleKeys] = useState<ReadonlySet<string>>(() => new Set());

  // FlatList는 onViewableItemsChanged가 렌더링 중에 다른 함수로 바뀌면 에러를 내므로 항상 같은 함수를 넘긴다.
  const onViewableItemsChanged = useCallback(({ viewableItems }: { viewableItems: ViewToken[] }) => {
    const next = new Set<string>();
    for (const token of viewableItems) {
      if (token.isViewable) next.add(token.key);
    }
    // 보이는 행이 그대로면 같은 객체를 유지해 불필요한 다시 그리기를 막는다.
    setVisibleKeys((prev) => (sameKeys(prev, next) ? prev : next));
  }, []);

  return { visibleKeys, onViewableItemsChanged };
}
