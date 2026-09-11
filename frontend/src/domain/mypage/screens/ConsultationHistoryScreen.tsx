import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getConsultations } from '../api/consultation';
import { ConsultationSummary, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

// createdAt에 타임존이 없어서 Date로 파싱하면 기기 설정에 따라 날짜가 밀릴 수 있음->문자열을 자르기
function formatDate(createdAt: string) {
  return createdAt.slice(0, 10).replace(/-/g, '.');
}

export default function ConsultationHistoryScreen() {
  const navigation = useNavigation<Nav>();
  const [consultations, setConsultations] = useState<ConsultationSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setConsultations(await getConsultations());
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '상담 이력을 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  return (
    <View style={styles.container}>
      <AppHeader title="상담 이력" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.subRow}>
          <Text style={styles.subText}>지난 AI 증상 상담 기록</Text>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{consultations.length}건</Text>
          </View>
        </View>

        {loading ? (
          <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xl }} />
        ) : consultations.length === 0 ? (
          <View style={styles.emptyBox}>
            <Ionicons name="time-outline" size={36} color={colors.placeholder} />
            <Text style={styles.emptyText}>아직 상담 기록이 없어요</Text>
            <Text style={styles.emptySub}>AI상담 탭에서 증상을 물어보면 여기에 저장돼요</Text>
          </View>
        ) : (
          consultations.map((c) => (
            <Pressable
              key={c.sessionId}
              style={styles.card}
              onPress={() =>
                navigation.navigate('ConsultationDetail', {
                  sessionId: c.sessionId,
                  title: c.firstQuestion,
                })
              }
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.question} numberOfLines={2}>
                  {c.firstQuestion}
                </Text>
                <Text style={styles.date}>{formatDate(c.createdAt)}</Text>
              </View>
              <Ionicons name="chevron-forward" size={20} color={colors.placeholder} />
            </Pressable>
          ))
        )}

        <View style={styles.note}>
          <Text style={styles.noteText}>
            로그인 상태에서 진행한 상담만 저장됩니다. 이 기록은 참고용이며 진단이 아닙니다.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.md },
  subRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  subText: { fontSize: font.sub, color: colors.textSub },
  badge: {
    backgroundColor: colors.primary,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: 2,
  },
  badgeText: { color: colors.white, fontSize: font.caption, fontWeight: '700' },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.white,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.lg,
  },
  question: { fontSize: font.body, fontWeight: '600', color: colors.text },
  date: { fontSize: font.caption, color: colors.placeholder, marginTop: 4 },
  emptyBox: { alignItems: 'center', gap: spacing.sm, paddingVertical: spacing.xxl },
  emptyText: { fontSize: font.body, color: colors.textSub, fontWeight: '600' },
  emptySub: { fontSize: font.caption, color: colors.placeholder },
  note: { backgroundColor: colors.inputBg, borderRadius: radius.md, padding: spacing.lg },
  noteText: { fontSize: font.caption, color: colors.textSub, lineHeight: 18 },
});