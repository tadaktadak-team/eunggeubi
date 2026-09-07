import { Ionicons } from '@expo/vector-icons';
import { CompositeNavigationProp, useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { getConsultations } from '../api/consultation';
import { getGuardians } from '../api/guardian';
import { getHealthProfile } from '../api/health';
import { getMyInfo } from '../api/user';
import { ConsultationSummary, Guardian, HealthProfile, MyInfo, MyPageStackParamList } from '../types';
import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { useAuth } from '../../auth/hooks/useAuth';

type Nav = CompositeNavigationProp<
  NativeStackNavigationProp<MyPageStackParamList>,
  NativeStackNavigationProp<RootStackParamList>
>;
type IconName = keyof typeof Ionicons.glyphMap;

// 건강 프로필을 카드 한 줄짜리 요약 문구로 만든다. 항목이 많으면 앞의 3개만 보여주고 나머지는 개수로.
function buildHealthSummary(profile: HealthProfile | null): string {
  if (!profile) return '건강 프로필을 등록해보세요';

  const items = [
    profile.bloodType ? `혈액형 ${profile.bloodType}` : null,
    ...profile.diseases,
    ...profile.medications,
  ].filter((v): v is string => !!v);

  if (items.length === 0) return '건강 프로필을 등록해보세요';
  if (items.length <= 3) return items.join(' · ');
  return `${items.slice(0, 3).join(' · ')} 외 ${items.length - 3}건`;
}

const QUICK: { key: string; label: string; icon: IconName }[] = [
  { key: 'guardian', label: '보호자 관리', icon: 'people-outline' },
  { key: 'health', label: '건강 프로필', icon: 'heart-outline' },
  { key: 'history', label: '상담 이력', icon: 'time-outline' },
];

const MENUS: { key: string; label: string; sub?: string; icon: IconName }[] = [
  { key: 'password', label: '비밀번호 변경', icon: 'lock-closed-outline' },
  { key: 'terms', label: '이용약관 · 개인정보처리방침', icon: 'document-text-outline' },
];

export default function MyPageHomeScreen() {
  const navigation = useNavigation<Nav>();
  const { isLoggedIn, signOut } = useAuth();

  const [myInfo, setMyInfo] = useState<MyInfo | null>(null);
  const [guardians, setGuardians] = useState<Guardian[]>([]);
  const [health, setHealth] = useState<HealthProfile | null>(null);
  const [consultations, setConsultations] = useState<ConsultationSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      // 서로 의존하지 않는 요청이라 병렬로 부른다 (순차로 하면 대기 시간이 그만큼 늘어난다)
      const [info, guardianList, profile, consultationList] = await Promise.all([
        getMyInfo(),
        getGuardians(),
        getHealthProfile(),
        getConsultations(),
      ]);
      setMyInfo(info);
      setGuardians(guardianList);
      setHealth(profile);
      setConsultations(consultationList);
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '내 정보를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  // 보호자 관리·건강 프로필에서 수정하고 돌아오면 요약도 갱신돼야 하므로 포커스마다 다시 부른다.
  useFocusEffect(
    useCallback(() => {
      if (isLoggedIn) load();
    }, [isLoggedIn, load]),
  );

  const healthSummary = buildHealthSummary(health);
  const quickValues: Record<string, string> = {
    guardian: `${guardians.length}명`,
    health: health?.bloodType || health?.diseases.length || health?.medications.length ? '등록됨' : '미등록',
    history: `${consultations.length}회`,
  };
  // 소셜 전용 계정은 비밀번호가 없어서 변경 자체가 불가능하므로 메뉴에서 숨긴다.
  const menus = MENUS.filter((m) => !(m.key === 'password' && myInfo?.socialOnly));

  // TODO: 세부화면 만들면 navigation.navigate로 교체
  const go = (label: string) => Alert.alert(label, '준비 중입니다.');

  const onLogout = () =>
    Alert.alert('로그아웃', '로그아웃 하시겠어요?', [
      { text: '취소', style: 'cancel' },
      { text: '로그아웃', style: 'destructive', onPress: () => signOut() },
    ]);

  const onQuick = (key: string, label: string) => {
    if (key === 'guardian') navigation.navigate('Guardian');
    else if (key === 'health') navigation.navigate('Health');
    else if (key === 'history') navigation.navigate('ConsultationHistory');
    else go(label);
  };


  if (!isLoggedIn) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <View style={styles.guestBox}>
          <Ionicons name="lock-closed-outline" size={40} color={colors.placeholder} />
          <Text style={styles.guestText}>로그인이 필요한 화면이에요</Text>
          <Pressable style={styles.loginBtn} onPress={() => navigation.navigate('Login')}>
            <Text style={styles.loginBtnText}>로그인 / 회원가입</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  if (loading) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.pageTitle}>마이페이지</Text>
        </View>

        {/* 프로필 카드 */}
        <Pressable style={styles.profileCard} onPress={() => navigation.navigate('AccountEdit')}>
          <Ionicons name="person-circle-outline" size={48} color={colors.textSub} />
          <View style={{ flex: 1 }}>
            <Text style={styles.profileName}>{myInfo?.name ?? '-'} 님</Text>
            <Text style={styles.profileEmail}>{myInfo?.email ?? ''}</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color={colors.placeholder} />
        </Pressable>

        {/* 퀵 액션 (통계를 함께 표시해 별도 통계 줄 없이 진입점 하나로 합친다) */}
        <View style={styles.quickRow}>
          {QUICK.map((q) => (
            <Pressable key={q.key} style={styles.quickItem} onPress={() => onQuick(q.key, q.label)}>
              <Ionicons name={q.icon} size={24} color={colors.primary} />
              <Text style={styles.quickValue}>{quickValues[q.key]}</Text>
              <Text style={styles.quickLabel}>{q.label}</Text>
            </Pressable>
          ))}
        </View>

        {/* 건강 프로필 하이라이트 */}
        <Pressable style={styles.healthCard} onPress={() => navigation.navigate('Health')}>
          <Ionicons name="medkit-outline" size={22} color={colors.primary} />
          <View style={{ flex: 1 }}>
            <Text style={styles.healthTitle}>내 건강 프로필 확인하기</Text>
            <Text style={styles.healthSub}>{healthSummary}</Text>
          </View>
          <Ionicons name="chevron-forward" size={20} color={colors.primary} />
        </Pressable>

        {/* 메뉴 리스트 */}
        <View style={styles.menuCard}>
          {menus.map((m, i) => (
            <Pressable
              key={m.key}
              style={[styles.menuRow, i > 0 && styles.menuBorder]}
              onPress={() => (m.key === 'password' ? navigation.navigate('ChangePassword') : go(m.label))}
            >
              <Ionicons name={m.icon} size={20} color={colors.textSub} />
              <View style={{ flex: 1 }}>
                <Text style={styles.menuLabel}>{m.label}</Text>
                {m.sub && <Text style={styles.menuSub}>{m.sub}</Text>}
              </View>
              <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
            </Pressable>
          ))}
        </View>

        {/* 로그아웃 + 회원 탈퇴 + 버전 */}
        <View style={styles.footer}>
          <View style={styles.footerRow}>
            <Pressable onPress={onLogout}>
              <Text style={styles.logout}>로그아웃</Text>
            </Pressable>
            <Text style={styles.footerDivider}>·</Text>
            <Pressable onPress={() => navigation.navigate('Withdraw')}>
              <Text style={styles.logout}>회원 탈퇴</Text>
            </Pressable>
          </View>
          <Text style={styles.version}>v1.0.0</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.inputBg },
  content: { padding: spacing.lg, gap: spacing.md },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: spacing.xs },
  pageTitle: { fontSize: font.h1, fontWeight: '800', color: colors.text },

  profileCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    backgroundColor: colors.white,
    borderRadius: radius.md,
    padding: spacing.lg,
  },
  profileName: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  profileEmail: { fontSize: font.sub, color: colors.textSub, marginTop: 2 },

  quickRow: { flexDirection: 'row', backgroundColor: colors.white, borderRadius: radius.md, paddingVertical: spacing.lg },
  quickItem: { flex: 1, alignItems: 'center', gap: spacing.xs },
  quickValue: { fontSize: font.h3, fontWeight: '800', color: colors.text },
  quickLabel: { fontSize: font.caption, color: colors.textSub },


  healthCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    backgroundColor: colors.primaryLight,
    borderRadius: radius.md,
    padding: spacing.lg,
  },
  healthTitle: { fontSize: font.body, fontWeight: '700', color: colors.primaryDark },
  healthSub: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },

  menuCard: { backgroundColor: colors.white, borderRadius: radius.md, overflow: 'hidden' },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, padding: spacing.lg },
  menuBorder: { borderTopWidth: 1, borderTopColor: colors.border },
  menuLabel: { fontSize: font.body, color: colors.text, fontWeight: '600' },
  menuSub: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },

  footer: { alignItems: 'center', gap: spacing.xs, marginTop: spacing.md },
  footerRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  footerDivider: { color: colors.placeholder, fontSize: font.sub },
  logout: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
  version: { color: colors.placeholder, fontSize: font.caption },

  guestBox: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md, paddingHorizontal: spacing.xl },
  guestText: { fontSize: font.body, color: colors.textSub },
  loginBtn: { backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, height: 50, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginTop: spacing.md },
  loginBtnText: { color: colors.white, fontWeight: '700', fontSize: font.body },
});