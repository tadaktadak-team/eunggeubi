import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type IconSpec = { lib: 'ion'; name: keyof typeof Ionicons.glyphMap } | { lib: 'mci'; name: keyof typeof MaterialCommunityIcons.glyphMap };

type Props = {
  label: string;
  icon: IconSpec;
  onPress: () => void;
};

// 홈 화면의 "바로가기" 3분할 카드 (병원찾기 / 약국찾기 / 약물정보 등)
export default function QuickLinkCard({ label, icon, onPress }: Props) {
  return (
    <Pressable style={styles.card} onPress={onPress}>
      {icon.lib === 'ion' ? (
        <Ionicons name={icon.name} size={22} color={colors.textSub} />
      ) : (
        <MaterialCommunityIcons name={icon.name} size={22} color={colors.textSub} />
      )}
      <Text style={styles.label}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    paddingVertical: spacing.lg,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
  },
  label: { fontSize: font.caption, color: colors.textSub, fontWeight: '600' },
});
