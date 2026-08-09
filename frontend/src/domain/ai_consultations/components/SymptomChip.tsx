import { Pressable, StyleSheet, Text } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type Props = {
  label: string;
  onPress: () => void;
  active?: boolean; // true면 선택된 상태로 강조 표시 (예: 응급처치 안내의 선택된 상황)
};

// 홈 화면의 "두통 · 복통 · 발열 · 어지럼" 빠른 선택 칩 / 응급처치 안내의 상황 선택 칩
export default function SymptomChip({ label, onPress, active = false }: Props) {
  return (
    <Pressable style={[styles.chip, active && styles.chipActive]} onPress={onPress}>
      <Text style={[styles.text, active && styles.textActive]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.white,
  },
  chipActive: { borderColor: colors.primary },
  text: { fontSize: font.sub, color: colors.text, fontWeight: '600' },
  textActive: { color: colors.primary },
});
