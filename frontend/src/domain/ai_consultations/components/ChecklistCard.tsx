import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import Button from '../../../shared/components/Button';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { ChecklistChatMessage } from '../types';
import AiAvatar from './AiAvatar';

type Props = {
  message: ChecklistChatMessage;
  onToggleItem: (itemId: string) => void;
  onSubmit: () => void;
  onSubmitNone: () => void;
};

export default function ChecklistCard({ message, onToggleItem, onSubmit, onSubmitNone }: Props) {
  return (
    <View style={styles.row}>
      <AiAvatar />
      <View style={styles.card}>
        <Text style={styles.title}>{message.title}</Text>

        {message.items.map((item) => (
          <Pressable
            key={item.id}
            style={styles.itemRow}
            onPress={() => onToggleItem(item.id)}
            disabled={message.answered}
            hitSlop={4}
          >
            <View style={[styles.checkbox, item.checked && styles.checkboxChecked]}>
              {item.checked && <Ionicons name="checkmark" size={14} color={colors.white} />}
            </View>
            <Text style={styles.itemLabel}>{item.label}</Text>
          </Pressable>
        ))}

        {message.answered ? (
          // 이미 답한 상태다. 눌리지 않지만 회색으로 덮지 않는다.
          <Button title="답변 완료" variant="neutral" size="md" done style={styles.submitBtn} />
        ) : (
          <View style={styles.buttonRow}>
            <Button
              title="해당사항 없음"
              variant="secondary"
              size="md"
              onPress={onSubmitNone}
              style={styles.rowBtn}
            />
            <Button title="답변하기" size="md" onPress={onSubmit} style={styles.rowBtn} />
          </View>
        )}
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
  title: { fontSize: font.body, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  itemRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  checkbox: {
    width: 20,
    height: 20,
    borderRadius: radius.sm,
    borderWidth: 1.5,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxChecked: { backgroundColor: colors.primary, borderColor: colors.primary },
  itemLabel: { fontSize: font.sub, color: colors.text, flexShrink: 1 },
  submitBtn: { marginTop: spacing.xs },
  buttonRow: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.xs },
  rowBtn: { flex: 1 },
});
