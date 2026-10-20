import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { Feather } from '@expo/vector-icons';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import DrugImage from './DrugImage';

interface DrugCardProps {
  name: string;         // 예: 타이레놀정500밀리그람
  drugType?: string;    // 예: 일반의약품
  itemImage?: string;   // 알약 이미지 URL (없거나 못 불러오면 제형 추정 아이콘으로 대체)
  onPress?: () => void;
  imageOrder?: number;  // 목록에서의 순서. 위쪽 카드 이미지부터 받도록 대기열 순서에 쓴다.
  imageActive?: boolean; // false면 이미지를 요청하지 않는다(화면이 다른 화면에 가려져 있을 때 등).
}

const DrugCard = ({ name, drugType, itemImage, onPress, imageOrder, imageActive = true }: DrugCardProps) => {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.7}>
      <DrugImage uri={itemImage} name={name} size={48} style={styles.image} order={imageOrder} active={imageActive} />

      <View style={styles.infoContainer}>
        <Text style={styles.nameText} numberOfLines={1}>
          {name}
        </Text>
        {drugType && <Text style={styles.drugTypeText}>{drugType}</Text>}
      </View>

      <Feather name="chevron-right" size={20} color={colors.placeholder} />
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
    marginBottom: spacing.md,
  },
  image: {
    marginRight: spacing.lg,
  },
  infoContainer: {
    flex: 1,
  },
  nameText: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
  },
  drugTypeText: {
    fontSize: font.caption,
    color: colors.textSub,
    marginTop: spacing.xs,
  },
});

export default DrugCard;
