import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { AnswerChatMessage, AnswerSource } from '../types';
import AiAvatar from './AiAvatar';

type Props = {
  message: AnswerChatMessage;
};

// 같은 문서(cntntsSn 기준, 없으면 disease·section·sourceName 조합)에서 나온 자료는 문장마다 따로
// 인용됐더라도 출처 목록에는 한 번만 보여준다 - 번호별로 늘어놓으면 사용자에게는 그냥 같은 자료가
// 여러 번 나열되는 것으로 보인다.
function dedupeSources(sources: AnswerSource[]): AnswerSource[] {
  const seen = new Set<string>();
  return sources.filter((source) => {
    const key = source.cntntsSn || `${source.disease}|${source.section}|${source.sourceName}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

// 백엔드가 문장 단위로 잘라준 답변(segments)을 이어서 보여준다. 문장별 인용 번호는 답변 본문에
// 노출하지 않고, 실제로 인용된 자료만 하단에 출처로 모아서 보여준다.
export default function AnswerCard({ message }: Props) {
  const sources = dedupeSources(message.sources);

  return (
    <View style={styles.row}>
      <AiAvatar />
      <View style={styles.card}>
        <Text style={styles.body}>
          {message.segments.map((segment) => segment.text).join(' ')}
        </Text>

        {sources.length > 0 && (
          <View style={styles.sourceBox}>
            <View style={styles.sourceHeader}>
              <Ionicons name="document-text-outline" size={12} color={colors.placeholder} />
              <Text style={styles.sourceHeaderText}>출처</Text>
            </View>
            {sources.map((source) => (
              <Text key={source.cntntsSn || `${source.disease}-${source.section}`} style={styles.sourceLine}>
                {source.disease} · {source.section}
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
