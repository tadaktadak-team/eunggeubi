import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { ReferenceChatMessage } from '../types';
import AiAvatar from './AiAvatar';

type Props = {
  message: ReferenceChatMessage;
};

export default function ReferenceInfoCard({ message }: Props) {
  return (
    <View style={styles.row}>
      <AiAvatar />
      <View style={styles.card}>
        <Text style={styles.title}>{message.title}</Text>
        <Text style={styles.body}>{message.body}</Text>
        <View style={styles.sourceRow}>
          <Ionicons name="document-text-outline" size={12} color={colors.placeholder} />
          <Text style={styles.source}>출처: {message.source}</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm },
  card: {
    flex: 1,
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    borderTopLeftRadius: radius.sm,
    padding: spacing.lg,
  },
  title: { fontSize: font.body, fontWeight: '700', color: colors.text, marginBottom: spacing.sm },
  body: { fontSize: font.sub, color: colors.textSub, lineHeight: 20 },
  sourceRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, marginTop: spacing.md },
  source: { fontSize: font.caption, color: colors.placeholder },
});
