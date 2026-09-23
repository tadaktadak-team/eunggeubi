import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useRoute } from '@react-navigation/native';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import EmergencyMapView from '../components/EmergencyMapView';
import { RootStackParamList } from '../../../navigation/types';
import { GuardianResult, Relationship } from '../types';
import { colors } from '../../../shared/theme/theme';

const RELATIONSHIP_LABEL: Record<Relationship, string> = {
  PARENT: '부모',
  GRANDPARENT: '조부모',
  SIBLING: '형제자매',
  OTHER: '기타',
};

type ResultRoute = RouteProp<RootStackParamList, 'EmergencyResult'>;

// 발송 결과를 그대로 반영한다. 실제로 못 갔는데 갔다고 보이면 사용자가 도움을 기다리게 된다.
function buildSummary(guardians: GuardianResult[]) {
  const sent = guardians.filter((g) => g.status === 'SENT').length;
  const failed = guardians.length - sent;

  if (guardians.length === 0) {
    return {
      ok: false,
      icon: 'alert-circle' as const,
      title: '알림을 보낼 보호자가 없어요',
      sub: '마이페이지 > 보호자 관리에서 보호자를 등록해주세요.',
    };
  }
  if (sent === 0) {
    return {
      ok: false,
      icon: 'close-circle' as const,
      title: '보호자에게 알림을 보내지 못했어요',
      sub: '직접 연락하시거나 119로 전화해주세요.',
    };
  }
  if (failed > 0) {
    return {
      ok: false,
      icon: 'alert-circle' as const,
      title: `보호자 ${sent}명에게 알림 발송됨`,
      sub: `${failed}명은 전송에 실패했어요. 직접 연락이 필요해요.`,
    };
  }
  return { ok: true, icon: 'checkmark' as const, title: '보호자에게 알림 발송됨', sub: null };
}

export default function EmergencyResultScreen() {
  const { params } = useRoute<ResultRoute>();
  const { address, message, guardians, latitude, longitude } = params;

  // 문자 원문에서 링크 줄은 뺀다 (위치는 지도로 직접 보여준다)
  const messagePreview = message
    .split('\n')
    .filter((line) => !line.includes('http'))
    .join('\n')
    .trim();

  const summary = buildSummary(guardians);
  const accent = summary.ok ? colors.success : colors.danger;

  return (
    <View style={styles.container}>
      <AppHeader title="긴급 알림" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={[styles.checkCircle, { borderColor: accent }]}>
          <Ionicons name={summary.icon} size={40} color={accent} />
        </View>
        <Text style={styles.title}>{summary.title}</Text>
        {summary.sub && <Text style={styles.subtitle}>{summary.sub}</Text>}

        <View style={styles.mapCard}>
          <EmergencyMapView latitude={latitude} longitude={longitude} />
          <View style={styles.locationBadge}>
            <Ionicons name="location" size={14} color={colors.white} />
            <Text style={styles.locationBadgeText}>현재 위치</Text>
          </View>
        </View>
        <Text style={styles.address}>{address}</Text>

        {guardians.length > 0 && (
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Ionicons name="notifications-outline" size={16} color={colors.textSub} />
              <Text style={styles.cardHeaderText}>발송 메시지</Text>
            </View>
            <Text style={styles.message}>{messagePreview}</Text>
          </View>
        )}

        {guardians.map((g) => (
          <View key={g.phone} style={styles.guardianRow}>
            <Ionicons name="person-outline" size={18} color={colors.textSub} />
            <Text style={styles.guardianName}>
              {g.name} ({RELATIONSHIP_LABEL[g.relationship]})
            </Text>
            {g.status === 'SENT' ? (
              <Text style={styles.sent}>전송됨 ✓</Text>
            ) : (
              <Text style={styles.failed}>전송 실패</Text>
            )}
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white },
  content: { padding: 20, alignItems: 'center' },
  checkCircle: {
    width: 80,
    height: 80,
    borderRadius: 40,
    borderWidth: 3,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 16,
  },
  title: { fontSize: 20, fontWeight: 'bold', marginTop: 16, textAlign: 'center' },
  subtitle: {
    fontSize: 14,
    color: colors.textSub,
    marginTop: 8,
    textAlign: 'center',
    lineHeight: 20,
  },
  mapCard: {
    width: '100%',
    height: 200,
    borderRadius: 12,
    overflow: 'hidden',
    marginTop: 20,
    backgroundColor: colors.inputBg,
  },
  locationBadge: {
    position: 'absolute',
    top: 10,
    left: 10,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.primary,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    gap: 4,
  },
  locationBadgeText: { color: colors.white, fontWeight: 'bold', fontSize: 13 },
  address: { marginTop: 10, marginBottom: 20, fontSize: 15, color: colors.text },
  card: {
    width: '100%',
    backgroundColor: colors.inputBg,
    borderRadius: 12,
    padding: 14,
    marginBottom: 16,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 },
  cardHeaderText: { color: colors.textSub, fontSize: 13, fontWeight: 'bold' },
  message: { color: colors.textSub, fontSize: 14, lineHeight: 20 },
  guardianRow: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
    gap: 8,
  },
  guardianName: { flex: 1, fontSize: 15, color: colors.text },
  sent: { color: colors.success, fontWeight: 'bold', fontSize: 13 },
  failed: { color: colors.danger, fontWeight: 'bold', fontSize: 13 },
});