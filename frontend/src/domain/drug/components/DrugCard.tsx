import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image } from 'react-native';
import { MaterialCommunityIcons, Feather } from '@expo/vector-icons';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getDrugFormIconName } from '../utils/drugIcon';

interface DrugCardProps {
  name: string;         // 예: 타이레놀정500밀리그람
  drugType?: string;    // 예: 일반의약품
  itemImage?: string;   // 알약 이미지 URL (없으면 제형 추정 아이콘으로 대체)
  onPress?: () => void;
}

const DrugCard = ({ name, drugType, itemImage, onPress }: DrugCardProps) => {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.7}>
      <View style={styles.iconContainer}>
        {itemImage ? (
          <Image source={{ uri: itemImage }} style={styles.image} />
        ) : (
          <MaterialCommunityIcons name={getDrugFormIconName(name)} size={24} color={colors.primary} />
        )}
      </View>

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
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.lg,
    overflow: 'hidden',
  },
  image: {
    width: 48,
    height: 48,
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
