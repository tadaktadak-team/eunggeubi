import React, { useEffect, useRef, useState } from 'react';
import { View, Image, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { colors, radius } from '../../../shared/theme/theme';
import { getDrugFormIconName } from '../utils/drugIcon';
import { imageLoadQueue, IMAGE_RETRY_DELAY_MS } from '../utils/imageLoadQueue';

// 식약처 약 사진은 전부 가로세로 비율이 같다(표본 확인: 1299x709, 780x426 모두 약 1.83:1).
// 상세 화면에서 이 비율을 그대로 쓰면 흰 여백 없이 꽉 차고 잘리는 부분도 없다.
const PHOTO_ASPECT_RATIO = 1299 / 709;

interface DrugImageProps {
  uri?: string | null;
  name: string;               // 이미지가 없거나 로딩 중/실패일 때 보여줄 제형 아이콘을 고르는 데 사용
  variant?: 'thumb' | 'hero'; // thumb: 목록용 정사각형, hero: 상세용 와이드
  size?: number;              // thumb일 때 한 변 길이
  style?: StyleProp<ViewStyle>;
}

// 약 이미지를 보여주는 곳이 목록/최근검색/낱알검색/상세로 흩어져 있어서 크기·여백·실패 처리가
// 제각각이었다. 식약처 이미지 서버는 첫 로딩이 수 초 걸리기도 하고 요청이 몰리면 429로 거절하기도
// 해서, 이미지가 올 때까지(또는 끝내 못 받을 때) 빈 분홍 박스만 보이는 문제가 있었다.
//
// 1) 제형 아이콘을 항상 바닥에 깔고 그 위에 이미지를 덮는다. 로딩 중에는 아이콘이, 로딩이
//    끝나면 이미지가, 실패하면 계속 아이콘이 보인다.
// 2) 이미지 요청은 imageLoadQueue를 거친다. 처음 몇 장은 바로, 그 뒤에는 일정한 속도로만
//    서버에 요청해서 429를 피한다. 허가를 기다리는 동안에도 아이콘이 보인다.
// 3) 실패하면(onError) 잠시 뒤 몇 번만 다시 시도하고, 그래도 안 되면 아이콘으로 둔다.
//    (RN의 onError는 HTTP 상태를 알려주지 않아 429와 404를 구분할 수 없다.)
const DrugImage = ({ uri, name, variant = 'thumb', size = 56, style }: DrugImageProps) => {
  // "허가받은 URL"과 "실패한 URL"을 저장한다. 값이 아니라 URL을 저장하면 uri가 바뀌었을 때
  // 별도 초기화 없이 이전 상태가 자연스럽게 무효가 된다.
  const [grantedUri, setGrantedUri] = useState<string | null>(null);
  const [failedUri, setFailedUri] = useState<string | null>(null);
  const [retryTick, setRetryTick] = useState(0);
  const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const priority = variant === 'hero' ? 'high' : 'normal';

  useEffect(() => {
    if (!uri || imageLoadQueue.hasGivenUp(uri)) return;

    let cancelled = false;
    let cancelAcquire: () => void = () => {};

    const acquire = () => {
      cancelAcquire = imageLoadQueue.acquire(
        uri,
        () => {
          if (!cancelled) setGrantedUri(uri);
        },
        priority,
      );
    };

    // 이미 기기 캐시에 있는 이미지는 서버 요청이 나가지 않으니 토큰을 쓰지 않고 바로 연다.
    // queryCache를 지원하지 않거나 실패하면 그냥 대기열로 간다.
    Promise.resolve()
      .then(() => {
        if (!Image.queryCache) throw new Error('Image.queryCache 미지원');
        return Image.queryCache([uri]);
      })
      .then((cache) => {
        if (cancelled) return;
        if (cache[uri]) {
          imageLoadQueue.markGranted(uri);
          setGrantedUri(uri);
        } else {
          acquire();
        }
      })
      .catch(() => {
        if (!cancelled) acquire();
      });

    return () => {
      cancelled = true;
      cancelAcquire();
    };
  }, [uri, priority, retryTick]);

  useEffect(
    () => () => {
      if (retryTimerRef.current) clearTimeout(retryTimerRef.current);
    },
    [],
  );

  const handleLoad = () => {
    if (uri) imageLoadQueue.recordSuccess(uri);
  };

  const handleError = () => {
    if (!uri) return;
    imageLoadQueue.forgetGranted(uri);
    imageLoadQueue.recordFailure(uri);
    setFailedUri(uri);
    if (!imageLoadQueue.hasGivenUp(uri)) {
      retryTimerRef.current = setTimeout(() => {
        setFailedUri(null);
        setGrantedUri(null);
        setRetryTick((tick) => tick + 1);
      }, IMAGE_RETRY_DELAY_MS);
    }
  };

  // 이미지를 보여줄 가능성이 있는가(URL이 있고, 실패/포기 상태가 아님).
  const maybeImage = !!uri && failedUri !== uri && !imageLoadQueue.hasGivenUp(uri);
  // 실제로 <Image>를 그려도 되는가(대기열에서 허가를 받음).
  const canLoad = maybeImage && grantedUri === uri;
  const iconName = getDrugFormIconName(name);

  if (variant === 'hero') {
    // 사진이 없거나 못 받으면 와이드 박스 대신 기존처럼 정사각형 아이콘 박스를 쓴다.
    if (!maybeImage) {
      return (
        <View style={[styles.heroFallback, style]}>
          <MaterialCommunityIcons name={iconName} size={56} color={colors.primary} />
        </View>
      );
    }
    return (
      <View style={[styles.hero, style]}>
        <MaterialCommunityIcons name={iconName} size={56} color={colors.primary} />
        {canLoad && (
          <Image
            source={{ uri: uri! }}
            style={styles.overlay}
            resizeMode="cover"
            onLoad={handleLoad}
            onError={handleError}
          />
        )}
      </View>
    );
  }

  return (
    <View style={[styles.thumb, { width: size, height: size }, style]}>
      <MaterialCommunityIcons name={iconName} size={Math.round(size * 0.5)} color={colors.primary} />
      {canLoad && (
        <Image
          source={{ uri: uri! }}
          style={styles.overlay}
          resizeMode="cover"
          onLoad={handleLoad}
          onError={handleError}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
  },
  thumb: {
    borderRadius: radius.sm,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
  },
  hero: {
    width: '100%',
    maxWidth: 480,
    aspectRatio: PHOTO_ASPECT_RATIO,
    borderRadius: radius.lg,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    overflow: 'hidden',
    marginBottom: 12,
  },
  heroFallback: {
    width: 120,
    height: 120,
    borderRadius: radius.lg + 8,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
});

export default DrugImage;
