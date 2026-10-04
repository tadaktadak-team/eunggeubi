import React, { useState } from 'react';
import { View, Image, StyleSheet, StyleProp, ViewStyle } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';

import { colors, radius } from '../../../shared/theme/theme';
import { getDrugFormIconName } from '../utils/drugIcon';

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
// 그래서 제형 아이콘을 항상 바닥에 깔아두고 그 위에 이미지를 덮는다. 로딩 중에는 아이콘이,
// 로딩이 끝나면 이미지가, 실패하면(onError) 계속 아이콘이 보인다.
const DrugImage = ({ uri, name, variant = 'thumb', size = 56, style }: DrugImageProps) => {
  // URL이 바뀌면 실패 기록도 자연스럽게 무효가 되도록, 실패 여부가 아니라 "실패한 URL"을 저장한다.
  const [failedUri, setFailedUri] = useState<string | null>(null);
  const showImage = !!uri && failedUri !== uri;
  const iconName = getDrugFormIconName(name);

  if (variant === 'hero') {
    // 사진이 없거나 못 받으면 와이드 박스 대신 기존처럼 정사각형 아이콘 박스를 쓴다.
    if (!showImage) {
      return (
        <View style={[styles.heroFallback, style]}>
          <MaterialCommunityIcons name={iconName} size={56} color={colors.primary} />
        </View>
      );
    }
    return (
      <View style={[styles.hero, style]}>
        <MaterialCommunityIcons name={iconName} size={56} color={colors.primary} />
        <Image
          source={{ uri: uri! }}
          style={styles.overlay}
          resizeMode="cover"
          onError={() => setFailedUri(uri!)}
        />
      </View>
    );
  }

  return (
    <View style={[styles.thumb, { width: size, height: size }, style]}>
      <MaterialCommunityIcons name={iconName} size={Math.round(size * 0.5)} color={colors.primary} />
      {showImage && (
        <Image
          source={{ uri: uri! }}
          style={styles.overlay}
          resizeMode="cover"
          onError={() => setFailedUri(uri!)}
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
