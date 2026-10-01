import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { ActivityIndicator, Alert, Linking, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { useNearestEmergencyBed } from '../../medical_locator/hooks/useNearestEmergencyBed';
import { callPhone, openDirections } from '../../medical_locator/utils/contact';
import DisclaimerFooter from '../components/DisclaimerFooter';
import QuickLinkCard from '../components/QuickLinkCard';
import SymptomChip from '../components/SymptomChip';
import { AiConsultationStackParamList, QUICK_SYMPTOMS } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;

export default function SymptomHomeScreen() {
  const navigation = useNavigation<Nav>();
  const insets = useSafeAreaInsets();
  const [text, setText] = useState('');
  const { state: nearestBed, retry: retryNearestBed } = useNearestEmergencyBed();

  const goAsk = (message: string) => {
    const trimmed = message.trim();
    if (!trimmed) {
      Alert.alert('증상을 입력해주세요', '예) 어젯밤부터 두통이 심해요');
      return;
    }
    navigation.navigate('SymptomChat', { initialMessage: trimmed });
  };

  // 다음 단계(병원찾기·약물정보 탭)에서 실제 목적지로 연결하면 됩니다.
  const goToTab = (tabName: string) => {
    (navigation.getParent() as any)?.navigate(tabName);
  };

  const notReady = () => Alert.alert('준비 중', '다음 단계에서 만들 거예요!');

  return (
      <View style={styles.container}>
        <View style={[styles.header, { paddingTop: insets.top + spacing.md }]}>
          <Text style={styles.title}>{'어디가\n불편하세요?'}</Text>
          <Pressable style={styles.bellBtn} onPress={notReady} hitSlop={8}>
            <Ionicons name="notifications-outline" size={22} color={colors.text} />
          </Pressable>
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <Text style={styles.subtitle}>증상을 말하면 AI가 참고정보를 안내해요</Text>

          <View style={styles.inputCard}>
            <TextInput
                style={styles.input}
                placeholder="예) “어젯밤부터 두통이 심해요”"
                placeholderTextColor={colors.placeholder}
                value={text}
                onChangeText={setText}
                multiline
            />
            <View style={styles.inputCardFooter}>
              <Pressable style={styles.askBtn} onPress={() => goAsk(text)}>
                <Text style={styles.askBtnText}>물어보기</Text>
              </Pressable>
            </View>
          </View>

          <View style={styles.chipRow}>
            {QUICK_SYMPTOMS.map((symptom) => (
                <SymptomChip key={symptom} label={symptom} onPress={() => goAsk(symptom)} />
            ))}
          </View>

          <Text style={styles.sectionTitle}>바로가기</Text>
          <View style={styles.quickLinkRow}>
            <QuickLinkCard label="병원찾기" icon={{ lib: 'ion', name: 'location-outline' }} onPress={() => goToTab('Hospital')} />
            <QuickLinkCard
                label="응급처치 안내"
                icon={{ lib: 'ion', name: 'alert-circle-outline' }}
                onPress={() => navigation.navigate('FirstAidGuide', {})}
            />
            <QuickLinkCard label="약물정보" icon={{ lib: 'mci', name: 'pill' }} onPress={() => goToTab('Medicine')} />
          </View>

          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitleInline}>가까운 응급실</Text>
            <Pressable onPress={() => goToTab('Hospital')} hitSlop={8}>
              <Text style={styles.moreLink}>전체 보기</Text>
            </Pressable>
          </View>
          {nearestBed.status === 'ready' ? (
            nearestBed.beds.map((bed) => (
              <View key={bed.hpid} style={styles.erBedCard}>
                <View style={styles.erBedTop}>
                  <View style={styles.erInfo}>
                    <Text style={styles.erName} numberOfLines={1}>
                      {bed.name}
                    </Text>
                    <Text style={styles.erMeta}>
                      {[
                        bed.distance != null && `${bed.distance}km`,
                        bed.congestion != null && `혼잡도 ${bed.congestion}%`,
                      ]
                        .filter(Boolean)
                        .join(' · ') || '거리 정보 없음'}
                    </Text>
                  </View>
                  <View style={styles.erBadge}>
                    <Text style={styles.erBadgeText}>병상 {bed.availableBeds ?? '-'}</Text>
                  </View>
                </View>
                <View style={styles.erActions}>
                  <Pressable
                    style={[styles.erActionBtn, !bed.phone && styles.erActionDisabled]}
                    onPress={() => callPhone(bed.phone)}
                    disabled={!bed.phone}
                  >
                    <Ionicons name="call-outline" size={16} color={colors.text} />
                    <Text style={styles.erActionText}>전화</Text>
                  </Pressable>
                  <Pressable
                    style={[styles.erActionBtn, styles.erActionPrimary, (bed.latitude == null || bed.longitude == null) && styles.erActionDisabled]}
                    onPress={() => bed.latitude != null && bed.longitude != null && openDirections(bed.name, bed.latitude, bed.longitude)}
                    disabled={bed.latitude == null || bed.longitude == null}
                  >
                    <Ionicons name="navigate-outline" size={16} color={colors.white} />
                    <Text style={[styles.erActionText, { color: colors.white }]}>길찾기</Text>
                  </Pressable>
                </View>
              </View>
            ))
          ) : nearestBed.status === 'loading' ? (
            <View style={[styles.erCard, styles.erLoading]}>
              <ActivityIndicator color={colors.primary} />
              <Text style={styles.erMeta}>가까운 응급실을 찾는 중...</Text>
            </View>
          ) : (
            <Pressable
              style={styles.erCard}
              onPress={nearestBed.status === 'denied' ? () => Linking.openSettings() : nearestBed.status === 'error' ? retryNearestBed : () => goToTab('Hospital')}
            >
              <Text style={styles.erMeta}>
                {nearestBed.status === 'denied'
                  ? '위치 권한을 허용하면 가까운 응급실을 보여드려요. (눌러서 설정 열기)'
                  : nearestBed.status === 'outside'
                    ? '응급이는 대한민국 안에서만 가까운 응급실을 찾을 수 있어요.'
                    : nearestBed.status === 'empty'
                      ? '근처 응급실 정보가 없어요.'
                      : '가까운 응급실을 불러오지 못했어요. (눌러서 다시 시도)'}
              </Text>
            </Pressable>
          )}
        </ScrollView>

        <DisclaimerFooter />
      </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.xl,
    paddingBottom: spacing.md,
  },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, lineHeight: 34 },
  bellBtn: {
    width: 40,
    height: 40,
    borderRadius: radius.pill,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollContent: { paddingHorizontal: spacing.xl, paddingBottom: spacing.lg },
  subtitle: { fontSize: font.sub, color: colors.textSub, marginBottom: spacing.lg },
  inputCard: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
    marginBottom: spacing.lg,
  },
  input: { fontSize: font.body, color: colors.text, minHeight: 44, textAlignVertical: 'top' },
  inputCardFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: spacing.md,
  },
  askBtn: {
    backgroundColor: colors.black,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.sm + 2,
    borderRadius: radius.md,
  },
  askBtnText: { color: colors.white, fontSize: font.sub, fontWeight: '700' },
  chipRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.xl },
  sectionTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  quickLinkRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.xl },
  erCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: spacing.md },
  sectionTitleInline: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  moreLink: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  erBedCard: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
    marginBottom: spacing.sm,
    gap: spacing.md,
  },
  erBedTop: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.md },
  erActions: { flexDirection: 'row', gap: spacing.sm },
  erActionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    height: 40,
    borderRadius: radius.md,
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
  },
  erActionPrimary: { backgroundColor: colors.primary, borderColor: colors.primary },
  erActionDisabled: { opacity: 0.4 },
  erActionText: { fontSize: font.sub, fontWeight: '700', color: colors.text },
  erInfo: { flex: 1, gap: spacing.xs },
  erLoading: { justifyContent: 'flex-start', gap: spacing.md },
  erName: { fontSize: font.body, fontWeight: '700', color: colors.text },
  erMeta: { fontSize: font.caption, color: colors.textSub },
  erBadge: {
    borderWidth: 1,
    borderColor: colors.primary,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  erBadgeText: { color: colors.primary, fontSize: font.caption, fontWeight: '700' },
});