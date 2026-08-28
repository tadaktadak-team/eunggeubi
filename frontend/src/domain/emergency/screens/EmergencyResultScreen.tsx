import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useRoute } from '@react-navigation/native';
import { ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { RootStackParamList } from '../../../navigation/types';
import { Relationship } from '../types';
import { colors } from '../../../shared/theme/theme';

const RELATIONSHIP_LABEL: Record<Relationship, string> = {
  PARENT: '부모',
  GRANDPARENT: '조부모',
  SIBLING: '형제자매',
  OTHER: '기타',
};

type ResultRoute = RouteProp<RootStackParamList, 'EmergencyResult'>;

export default function EmergencyResultScreen() {
  const { params } = useRoute<ResultRoute>();
  const { address, message, guardians } = params;

  return (
    <View style={styles.container}>
      <AppHeader title="긴급 알림" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.checkCircle}>
          <Ionicons name="checkmark" size={40} color={colors.primary} />
        </View>
        <Text style={styles.title}>보호자에게 알림 발송됨</Text>

        <View style={styles.locationBadge}>
          <Ionicons name="location" size={14} color={colors.white} />
          <Text style={styles.locationBadgeText}>현재 위치</Text>
        </View>
        <Text style={styles.address}>{address}</Text>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <Ionicons name="notifications-outline" size={16} color={colors.textSub} />
            <Text style={styles.cardHeaderText}>발송 메시지</Text>
          </View>
          <Text style={styles.message}>“{message}”</Text>
        </View>

        {guardians.map((g) => (
          <View key={g.phone} style={styles.guardianRow}>
            <Ionicons name="person-outline" size={18} color={colors.textSub} />
            <Text style={styles.guardianName}>
              {g.name} ({RELATIONSHIP_LABEL[g.relationship]})
            </Text>
            <Text style={styles.sent}>전송됨 ✓</Text>
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
    borderColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 16,
  },
  title: { fontSize: 20, fontWeight: 'bold', marginTop: 16, marginBottom: 20 },
  locationBadge: {
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
});