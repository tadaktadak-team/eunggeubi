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

// 프로필 카드에 얹을 건강 요약. 개수 대신 실제 값을 보여주고, 길면 앞의 2개만 남긴다.
// 값이 없는 항목은 줄 자체를 만들지 않는다 (빈 배열이 "없음"인지 "미입력"인지 구분할 수 없어서).
function buildHealthRows(profile: HealthProfile | null): { label: string; value: string }[] {
  if (!profile) return [];
  const rows: { label: string; value: string }[] = [];
  if (profile.bloodType) rows.push({ label: '혈액형', value: `${profile.bloodType}형` });

  const add = (label: string, items: string[]) => {
    if (items.length === 0) return;
    const head = items.slice(0, 2).join(', ');
    rows.push({ label, value: items.length > 2 ? `${head} 외 ${items.length - 2}` : head });
  };
  add('지병', profile.diseases);
  add('알레르기', profile.allergies);
  add('복용약', profile.medications);
  return rows;
}

const QUICK: { key: string; label: string; icon: IconName }[] = [
  { key: 'guardian', label: '보호자 관리', icon: 'people-outline' },
  { key: 'history', label: '상담 이력', icon: 'time-outline' },
];

const MENUS: { key: string; label: string; sub?: string; icon: IconName }[] = [
  { key: 'password', label: '비밀번호 변경', icon: 'lock-closed-outline' },
  { key: 'terms', label: '이용약관 · 개인정보처리방침', icon: 'document-text-outline' },
  { key: 'support', label: '고객센터', sub: '문의 및 도움말', icon: 'headset-outline' },
  { key: 'notice', label: '공지사항', icon: 'megaphone-outline' },
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

  const healthRows = buildHealthRows(health);
  const quickValues: Record<string, string> = {
    guardian: `${guardians.length}명`,
    history: `${consultations.length}회`,
  };
  // 소셜 전용 계정은 비밀번호가 없어서 변경 자체가 불가능하므로 메뉴에서 숨긴다.
  const menus = MENUS.filter((m) => !(m.key === 'password' && myInfo?.socialOnly));

  const onLogout = () =>
    Alert.alert('로그아웃', '로그아웃 하시겠어요?', [
      { text: '취소', style: 'cancel' },
      { text: '로그아웃', style: 'destructive', onPress: () => signOut() },
    ]);

  const onMenu = (key: string, label: string) => {
    if (key === 'password') navigation.navigate('ChangePassword');
    else if (key === 'terms') navigation.navigate('Legal');
    else Alert.alert(label, '준비 중입니다.');
  };

  const onQuick = (key: string) => {
    if (key === 'guardian') navigation.navigate('Guardian');
    else if (key === 'history') navigation.navigate('ConsultationHistory');
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
          <Pressable onPress={onLogout} hitSlop={8}>
            <Ionicons name="log-out-outline" size={24} color={colors.textSub} />
          </Pressable>
        </View>

        {/* 프로필 카드 (계정 정보 + 건강 요약을 한 카드로 묶는다) */}
        <View style={styles.profileCard}>
          <Pressable style={styles.profileTop} onPress={() => navigation.navigate('AccountEdit')}>
            <View style={{ flex: 1 }}>
              <Text style={styles.profileName}>{myInfo?.name ?? '-'} 님</Text>
              <Text style={styles.profileEmail}>{myInfo?.email ?? ''}</Text>
            </View>
            <Text style={styles.profileEdit}>수정</Text>
            <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
          </Pressable>

          <Pressable style={styles.healthBox} onPress={() => navigation.navigate('Health')}>
            <View style={styles.healthHeader}>
              <Ionicons name="medkit-outline" size={16} color={colors.primaryDark} />
              <Text style={styles.healthTitle}>내 건강 정보</Text>
              <Ionicons name="chevron-forward" size={16} color={colors.primaryDark} />
            </View>
            {healthRows.length > 0 ? (
              healthRows.map((r) => (
                <View key={r.label} style={styles.healthRow}>
                  <Text style={styles.healthLabel}>{r.label}</Text>
                  <Text style={styles.healthValue} numberOfLines={1}>
                    {r.value}
                  </Text>
                </View>
              ))
            ) : (
              <Text style={styles.healthEmpty}>건강 프로필을 등록해보세요</Text>
            )}
          </Pressable>
        </View>

        {/* 퀵 액션 (통계를 함께 표시해 별도 통계 줄 없이 진입점 하나로 합친다) */}
        <View style={styles.quickRow}>
          {QUICK.map((q) => (
            <Pressable key={q.key} style={styles.quickItem} onPress={() => onQuick(q.key)}>
              <Ionicons name={q.icon} size={24} color={colors.primary} />
              <Text style={styles.quickValue}>{quickValues[q.key]}</Text>
              <Text style={styles.quickLabel}>{q.label}</Text>
            </Pressable>
          ))}
        </View>

        {/* 메뉴 리스트 */}
        <View style={styles.menuCard}>
          {menus.map((m, i) => (
            <Pressable
              key={m.key}
              style={[styles.menuRow, i > 0 && styles.menuBorder]}
              onPress={() => onMenu(m.key, m.label)}
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

        {/* 회원 탈퇴 + 버전 */}
        <View style={styles.footer}>
          <Pressable onPress={() => navigation.navigate('Withdraw')}>
            <Text style={styles.withdraw}>회원 탈퇴</Text>
          </Pressable>
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

  profileCard: { backgroundColor: colors.white, borderRadius: radius.md, overflow: 'hidden' },
  profileTop: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    padding: spacing.lg,
  },
  profileName: { fontSize: font.h2, fontWeight: '800', color: colors.text },
  profileEmail: { fontSize: font.sub, color: colors.textSub, marginTop: 2 },
  profileEdit: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },

  healthBox: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.primaryLight,
    gap: spacing.xs,
  },
  healthHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.xs, marginBottom: 2 },
  healthTitle: { flex: 1, fontSize: font.sub, fontWeight: '700', color: colors.primaryDark },
  healthRow: { flexDirection: 'row', alignItems: 'center' },
  healthLabel: { width: 60, fontSize: font.sub, color: colors.textSub },
  healthValue: { flex: 1, fontSize: font.sub, color: colors.text, fontWeight: '600' },
  healthEmpty: { fontSize: font.sub, color: colors.textSub },

  quickRow: { flexDirection: 'row', backgroundColor: colors.white, borderRadius: radius.md, paddingVertical: spacing.lg },
  quickItem: { flex: 1, alignItems: 'center', gap: spacing.xs },
  quickValue: { fontSize: font.h3, fontWeight: '800', color: colors.text },
  quickLabel: { fontSize: font.caption, color: colors.textSub },

  menuCard: { backgroundColor: colors.white, borderRadius: radius.md, overflow: 'hidden' },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, padding: spacing.lg },
  menuBorder: { borderTopWidth: 1, borderTopColor: colors.border },
  menuLabel: { fontSize: font.body, color: colors.text, fontWeight: '600' },
  menuSub: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },

  footer: { alignItems: 'center', gap: spacing.xs, marginTop: spacing.md },
  withdraw: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
  version: { color: colors.placeholder, fontSize: font.caption },

  guestBox: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md, paddingHorizontal: spacing.xl },
  guestText: { fontSize: font.body, color: colors.textSub },
  loginBtn: { backgroundColor: colors.primary, paddingHorizontal: spacing.xxl, height: 50, borderRadius: radius.md, alignItems: 'center', justifyContent: 'center', marginTop: spacing.md },
  loginBtnText: { color: colors.white, fontWeight: '700', fontSize: font.body },
});
