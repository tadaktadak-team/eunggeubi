import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { AnswerChatMessage } from '../types';
import AiAvatar from './AiAvatar';

type Props = {
  message: AnswerChatMessage;
};

// 백엔드가 문장 단위로 잘라준 답변(segments)을 이어서 보여주고, 각 문장 뒤에 그 문장이
// 인용한 참고자료 번호를 작은 배지로 붙인다. 하단에는 실제로 인용된 자료만 출처로 나열한다.
export default function AnswerCard({ message }: Props) {
  return (
    <View style={styles.row}>
      <AiAvatar />
      <View style={styles.card}>
        <Text style={styles.body}>
          {message.segments.map((segment, i) => (
            <Text key={i}>
              {segment.text}
              {segment.sourceIndexes.length > 0 && (
                <Text style={styles.citation}>
                  {' '}
                  {segment.sourceIndexes.map((n) => `[${n}]`).join('')}
                </Text>
              )}
              {i < message.segments.length - 1 ? ' ' : ''}
            </Text>
          ))}
        </Text>

        {message.sources.length > 0 && (
          <View style={styles.sourceBox}>
            <View style={styles.sourceHeader}>
              <Ionicons name="document-text-outline" size={12} color={colors.placeholder} />
              <Text style={styles.sourceHeaderText}>출처</Text>
            </View>
            {message.sources.map((source) => (
              <Text key={source.index} style={styles.sourceLine}>
                [{source.index}] {source.disease} · {source.section} ({source.sourceName})
              </Text>
            ))}
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
  body: { fontSize: font.sub, color: colors.text, lineHeight: 20 },
  citation: { fontSize: font.caption, color: colors.primary, fontWeight: '700' },
  sourceBox: {
    marginTop: spacing.md,
    paddingTop: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    gap: spacing.xs,
  },
  sourceHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, marginBottom: spacing.xs },
  sourceHeaderText: { fontSize: font.caption, color: colors.placeholder, fontWeight: '700' },
  sourceLine: { fontSize: font.caption, color: colors.placeholder, lineHeight: 16 },
});
