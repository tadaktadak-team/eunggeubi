import React, { useEffect, useRef, useState } from 'react';
import { View, Image, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { colors, radius } from '../../../shared/theme/theme';
import { getDrugFormIconName } from '../utils/drugIcon';
import { imageLoadQueue } from '../utils/imageLoadQueue';

// 식약처 약 사진은 전부 가로세로 비율이 같다(표본 확인: 1299x709, 780x426 모두 약 1.83:1).
// 상세 화면에서 이 비율을 그대로 쓰면 흰 여백 없이 꽉 차고 잘리는 부분도 없다.
const PHOTO_ASPECT_RATIO = 1299 / 709;

interface DrugImageProps {
  uri?: string | null;
  name: string;               // 이미지가 없거나 로딩 중/실패일 때 보여줄 제형 아이콘을 고르는 데 사용
  variant?: 'thumb' | 'hero'; // thumb: 목록용 정사각형, hero: 상세용 와이드
  size?: number;              // thumb일 때 한 변 길이
  style?: StyleProp<ViewStyle>;
  // 지금 화면에 보이는 행인지. 목록(FlatList)은 화면 위아래 여러 화면 분량을 미리 그려 두는데, 그 행들까지
  // 전부 이미지를 요청하면 대기열이 수십 개씩 쌓여 정작 보이는 행이 뒤로 밀린다. false면 요청하지 않고
  // 아이콘만 보여주며, 이미 받은 사진은 화면 밖으로 나가도 그대로 둔다(다시 받으면 또 요청이 나가므로).
  active?: boolean;
  order?: number; // 목록에서의 행 번호. 위쪽 행부터 받도록 대기열 순서에 쓴다.
}

// 약 이미지를 보여주는 곳이 목록/최근검색/낱알검색/상세로 흩어져 있어서 크기·여백·실패 처리가
// 제각각이었다. 식약처 이미지 서버는 첫 로딩이 수 초 걸리기도 하고 요청이 몰리면 429로 거절하기도
// 해서, 이미지가 올 때까지(또는 끝내 못 받을 때) 빈 분홍 박스만 보이는 문제가 있었다.
//
// 1) 제형 아이콘을 항상 바닥에 깔고 그 위에 이미지를 덮는다. 로딩 중에는 아이콘이, 로딩이
//    끝나면 이미지가, 실패하면 계속 아이콘이 보인다.
// 2) 이미지 요청은 imageLoadQueue를 거친다. 처음 몇 장은 바로, 그 뒤에는 일정한 속도로만
//    서버에 요청해서 429를 피한다. 허가를 기다리는 동안에도 아이콘이 보인다.
// 3) 이미지가 실패하면(onError) 그 이미지만 잠시 뒤 다시 시도한다. 여러 장이 연달아 실패하면
//    대기열이 서버 거절로 판단해 기기 전체의 요청을 잠시(15초) 멈추고, 쉬고 나면 다시 받는다.
//    몇 번 더 실패하면 포기하고 아이콘으로 둔다.
//    (RN의 onError는 HTTP 상태를 알려주지 않아 429와 404를 구분할 수 없다.)
// 기기 캐시 조회(queryCache)로 대기열을 건너뛰던 처리는 없앴다. 웹에서는 "이번 실행에서 불러온 적
// 있는 URL"이면 있다고 답하지만, 서버가 no-store라 실제로는 다시 받으면서 요청 한도를 넘겼다.
const DrugImage = ({ uri, name, variant = 'thumb', size = 56, style, active = true, order }: DrugImageProps) => {
  // "허가받은 URL"과 "실패한 URL"을 저장한다. 값이 아니라 URL을 저장하면 uri가 바뀌었을 때
  // 별도 초기화 없이 이전 상태가 자연스럽게 무효가 된다.
  const [grantedUri, setGrantedUri] = useState<string | null>(null);
  const [failedUri, setFailedUri] = useState<string | null>(null);
  const cancelRetryRef = useRef<(() => void) | null>(null);
  const priority = variant === 'hero' ? 'high' : 'normal';

  useEffect(() => {
    // 보이지 않는 행, 이미 허가받은 이미지, 실패 후 재시도를 기다리는 이미지, 포기한 이미지는 요청하지 않는다.
    if (!uri || !active || grantedUri === uri || failedUri === uri || imageLoadQueue.hasGivenUp(uri)) return;

    let cancelled = false;
    const cancelAcquire = imageLoadQueue.acquire(
      uri,
      () => {
        if (!cancelled) setGrantedUri(uri);
      },
      { priority, order },
    );
    // 허가 전에 화면 밖으로 나가거나 사라지면 대기 요청을 취소한다(토큰을 쓰지 않음).
    return () => {
      cancelled = true;
      cancelAcquire();
    };
  }, [uri, active, priority, order, grantedUri, failedUri]);

  useEffect(
    () => () => {
      if (cancelRetryRef.current) cancelRetryRef.current();
    },
    [],
  );

  const handleLoad = () => {
    if (uri) imageLoadQueue.recordSuccess(uri);
  };

  const handleError = () => {
    if (!uri) return;
    // 실패를 알리면 대기열이 여러 장의 연속 실패인지(서버 거절) 판단한다. 이 이미지는 재시도해도 되는
    // 시점(평소엔 잠시 뒤, 정지 중이면 정지가 끝날 때)에 다시 대기열에 올린다.
    imageLoadQueue.reportFailure(uri);
    setFailedUri(uri);
    setGrantedUri(null);
    if (cancelRetryRef.current) cancelRetryRef.current();
    cancelRetryRef.current = null;
    if (!imageLoadQueue.hasGivenUp(uri)) {
      cancelRetryRef.current = imageLoadQueue.scheduleRetry(() => {
        cancelRetryRef.current = null;
        setFailedUri(null);
      });
    }
  };

  // 사진을 보여줄 가능성이 있는가(URL이 있고, 포기한 이미지가 아님). 재시도를 기다리는 동안에도 true라
  // 상세 사진 자리가 정사각형 박스로 바뀌었다 돌아오는 깜빡임이 없다.
  const maybeImage = !!uri && !imageLoadQueue.hasGivenUp(uri);
  // 실제로 <Image>를 그려도 되는가(대기열에서 허가를 받았고, 실패 후 재시도 대기 중이 아님).
  const canLoad = maybeImage && grantedUri === uri && failedUri !== uri;
  const iconName = getDrugFormIconName(name);

  if (variant === 'hero') {
    // 사진이 없거나 끝내 못 받으면 와이드 박스 대신 기존처럼 정사각형 아이콘 박스를 쓴다.
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
