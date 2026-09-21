import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useRoute } from '@react-navigation/native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getConsultationDetail } from '../api/consultation';
import { ConsultationMessage, MyPageStackParamList } from '../types';

type DetailRoute = RouteProp<MyPageStackParamList, 'ConsultationDetail'>;

function formatDate(createdAt: string) {
  return createdAt.slice(0, 10).replace(/-/g, '.');
}

function MessageBubble({ message }: { message: ConsultationMessage }) {
  if (message.senderType === 'USER') {
    return (
      <View style={styles.userRow}>
        <View style={styles.userBubble}>
          <Text style={styles.userText}>{message.content}</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={{ gap: spacing.md }}>
      <View style={styles.aiRow}>
        <View style={styles.avatar}>
          <Ionicons name="heart" size={14} color={colors.primary} />
        </View>
        <View style={styles.aiBubble}>
          {message.regenerated && (
            <View style={styles.regenChip}>
              <Text style={styles.regenChipText}>체크리스트 반영 답변</Text>
            </View>
          )}
          <Text style={styles.aiText}>{message.content}</Text>
          {!!message.disclaimer && <Text style={styles.disclaimerText}>{message.disclaimer}</Text>}
        </View>
      </View>

      {message.checklistAnswered && (
        <View style={styles.userRow}>
          <View style={[styles.userBubble, { gap: spacing.xs }]}>
            {message.checkedItems.length > 0 ? (
              message.checkedItems.map((item) => (
                <View key={item} style={styles.checkedRow}>
                  <Ionicons name="checkmark" size={15} color={colors.white} />
                  <Text style={[styles.userText, { flexShrink: 1 }]}>{item}</Text>
                </View>
              ))
            ) : (
              <Text style={styles.userText}>해당하는 항목이 없어요</Text>
            )}
          </View>
        </View>
      )}
    </View>
  );
}

export default function ConsultationDetailScreen() {
  const { params } = useRoute<DetailRoute>();
  const { sessionId, title } = params;

  const [messages, setMessages] = useState<ConsultationMessage[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const detail = await getConsultationDetail(sessionId);
      setMessages(detail.messages);
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '상담 내역을 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  // 지난 기록은 바뀌지 않으므로 진입할 때 한 번만 부른다.
  useEffect(() => {
    load();
  }, [load]);

  const firstDate = messages.length > 0 ? formatDate(messages[0].createdAt) : '';

  return (
    <View style={styles.container}>
      <AppHeader title="상담 내역" />
      <ScrollView contentContainerStyle={styles.content}>
        {/* 목록에서 받은 질문을 먼저 띄워 로딩 중에도 어떤 상담인지 알 수 있게 한다 */}
        <View style={styles.summary}>
          <Text style={styles.summaryTitle} numberOfLines={2}>
            {title}
          </Text>
          {!!firstDate && <Text style={styles.summaryDate}>{firstDate}</Text>}
        </View>

        {loading ? (
          <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xl }} />
        ) : (
          messages.map((m) => <MessageBubble key={m.id} message={m} />)
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.md },

  summary: {
    backgroundColor: colors.primaryLight,
    borderRadius: radius.md,
    padding: spacing.lg,
    gap: 4,
  },
  summaryTitle: { fontSize: font.body, fontWeight: '700', color: colors.primaryDark },
  summaryDate: { fontSize: font.caption, color: colors.textSub },

  userRow: { flexDirection: 'row', justifyContent: 'flex-end' },
  userBubble: {
    maxWidth: '80%',
    backgroundColor: colors.primary,
    borderRadius: radius.lg,
    borderTopRightRadius: radius.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  userText: { color: colors.white, fontSize: font.body, lineHeight: 20 },

  aiRow: { flexDirection: 'row', gap: spacing.sm, alignItems: 'flex-start' },
  avatar: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  aiBubble: {
    flex: 1,
    backgroundColor: colors.white,
    borderRadius: radius.lg,
    borderTopLeftRadius: radius.sm,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    gap: spacing.sm,
  },
  aiText: { color: colors.text, fontSize: font.body, lineHeight: 21 },
  disclaimerText: { fontSize: font.caption, color: colors.placeholder, fontStyle: 'italic' },
  regenChip: {
    alignSelf: 'flex-start',
    backgroundColor: colors.inputBg,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
  },
  regenChipText: { fontSize: font.caption, color: colors.textSub, fontWeight: '600' },

  checkedRow: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.xs },
});