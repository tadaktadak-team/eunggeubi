import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';

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
          <Pressable style={[styles.submitBtn, styles.submitBtnDisabled]} disabled>
            <Text style={styles.submitText}>답변 완료</Text>
          </Pressable>
        ) : (
          <View style={styles.buttonRow}>
            <Pressable style={[styles.submitBtn, styles.noneBtn]} onPress={onSubmitNone}>
              <Text style={styles.noneText}>해당사항 없음</Text>
            </Pressable>
            <Pressable style={[styles.submitBtn, styles.answerBtn]} onPress={onSubmit}>
              <Text style={styles.submitText}>답변하기</Text>
            </Pressable>
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
  submitBtn: {
    height: 44,
    borderRadius: radius.md,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.xs,
  },
  submitBtnDisabled: { backgroundColor: colors.disabled },
  submitText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  buttonRow: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.xs },
  answerBtn: { flex: 1, marginTop: 0 },
  noneBtn: {
    flex: 1,
    marginTop: 0,
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
  },
  noneText: { color: colors.textSub, fontSize: font.body, fontWeight: '700' },
});
