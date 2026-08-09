import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type Props = {
  text: string;
};

export default function ChatBubbleUser({ text }: Props) {
  return (
    <View style={styles.row}>
      <View style={styles.bubble}>
        <Text style={styles.text}>{text}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'flex-end' },
  bubble: {
    maxWidth: '80%',
    backgroundColor: colors.primary,
    borderRadius: radius.lg,
    borderTopRightRadius: radius.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  text: { color: colors.white, fontSize: font.body, lineHeight: 20 },
});
