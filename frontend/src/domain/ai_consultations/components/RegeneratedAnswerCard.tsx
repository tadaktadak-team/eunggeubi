import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { RegeneratedAnswerChatMessage, RegeneratedSource } from '../types';
import AiAvatar from './AiAvatar';

type Props = {
  message: RegeneratedAnswerChatMessage;
};

// 같은 자료가 여러 인용 id로 저장돼있어도 출처 목록에는 한 번만 보여준다.
function dedupeSources(sources: RegeneratedSource[]): RegeneratedSource[] {
  const seen = new Set<string>();
  return sources.filter((source) => {
    const key = `${source.title}|${source.urlOrOrg}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

// 체크리스트 응답을 반영해 재생성된 답변(POST /{id}/regenerate) 카드. 이 응답은 문장별 인용이 아니라
// 메시지 전체 하나 + flat 출처 목록이라 AnswerCard와 구조가 달라서 별도 컴포넌트로 뒀다.
export default function RegeneratedAnswerCard({ message }: Props) {
  const sources = dedupeSources(message.sources);

  return (
    <View style={styles.row}>
      <AiAvatar />
      <View style={styles.card}>
        <Text style={styles.body}>{message.message}</Text>

        {sources.length > 0 && (
          <View style={styles.sourceBox}>
            <View style={styles.sourceHeader}>
              <Ionicons name="document-text-outline" size={12} color={colors.placeholder} />
              <Text style={styles.sourceHeaderText}>출처</Text>
            </View>
            {sources.map((source) => (
              <Text key={source.referenceSourceId} style={styles.sourceLine}>
                {source.title}
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
