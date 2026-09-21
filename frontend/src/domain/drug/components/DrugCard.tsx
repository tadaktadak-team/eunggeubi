import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { MaterialCommunityIcons, Feather } from '@expo/vector-icons';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';

interface DrugCardProps {
  name: string;        // 예: 타이레놀정
  dosage: string;      // 예: 500mg
  category: string;    // 예: 해열·진통제
  onPress?: () => void;
}

const DrugCard = ({ name, dosage, category, onPress }: DrugCardProps) => {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.7}>
      <View style={styles.iconContainer}>
        <MaterialCommunityIcons name="pill" size={24} color={colors.primary} />
      </View>

      <View style={styles.infoContainer}>
        <Text style={styles.nameText}>{name}</Text>
        <Text style={styles.dosageText}>{dosage}</Text>
        <Text style={styles.categoryText}>{category}</Text>
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
  },
  infoContainer: {
    flex: 1,
  },
  nameText: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
  },
  dosageText: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
    marginTop: 2,
  },
  categoryText: {
    fontSize: font.caption,
    color: colors.textSub,
    marginTop: spacing.xs,
  },
});

export default DrugCard;
