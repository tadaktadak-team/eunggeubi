import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, Linking, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import DisclaimerFooter from '../components/DisclaimerFooter';
import SymptomChip from '../components/SymptomChip';
import { AiConsultationStackParamList, FIRST_AID_SITUATIONS, FirstAidSituation } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;
type GuideRoute = RouteProp<AiConsultationStackParamList, 'FirstAidGuide'>;

// TODO: 지금은 고정된 응급처치 문구입니다. 실제 의료 검수를 거친 콘텐츠(또는 백엔드 조회)로 교체가 필요해요.
const FIRST_AID_STEPS: Record<FirstAidSituation, string[]> = {
  화상: ['흐르는 찬물에 15분 식히기', '물집 터뜨리지 않기', '깨끗한 거즈로 덮기'],
  코피: ['고개를 앞으로 숙이기', '콧볼을 5~10분간 지그시 눌러주기', '얼음찜질로 혈관 수축시키기'],
  골절: ['다친 부위를 움직이지 않기', '부목 등으로 고정하기', '얼음찜질로 붓기 줄이기'],
  기도막힘: ['기침을 하도록 유도하기', '등을 세게 두드리기(견갑골 사이)', '하임리히법 시행하기'],
};

export default function FirstAidGuideScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<GuideRoute>();
  const insets = useSafeAreaInsets();

  const [searchText, setSearchText] = useState('');
  const [situation, setSituation] = useState<FirstAidSituation>(params?.situation ?? FIRST_AID_SITUATIONS[0]);

  const onSearchSubmit = () => {
    const trimmed = searchText.trim();
    if (!trimmed) return;

    const matched = FIRST_AID_SITUATIONS.find((s) => trimmed.includes(s) || s.includes(trimmed));
    if (matched) {
      setSituation(matched);
    } else {
      Alert.alert('안내', '해당 상황을 찾지 못했어요. 아래 목록에서 골라주세요.');
    }
  };

  const callEmergency = () => {
    if (__DEV__) {
      // 개발/테스트 중엔 실제 전화 대신 안내만 (emergency 도메인과 동일한 가드)
      Alert.alert('개발 모드', '실제 배포 앱에서는 119로 전화가 연결됩니다.');
      return;
    }
    Linking.openURL('tel:119').catch(() => {
      Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
    });
  };

  return (
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top }]}>
        <View style={styles.headerRow}>
          <Pressable style={styles.side} onPress={() => navigation.goBack()} hitSlop={8}>
            <Ionicons name="chevron-back" size={26} color={colors.text} />
          </Pressable>
          <Text style={styles.headerTitle}>응급처치 안내</Text>
          <View style={styles.side} />
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
        <View style={styles.searchBox}>
          <Ionicons name="search" size={18} color={colors.placeholder} />
          <TextInput
            style={styles.searchInput}
            placeholder="상황 입력 (예: 화상, 코피)"
            placeholderTextColor={colors.placeholder}
            value={searchText}
            onChangeText={setSearchText}
            onSubmitEditing={onSearchSubmit}
            returnKeyType="search"
          />
        </View>

        <View style={styles.chipRow}>
          {FIRST_AID_SITUATIONS.map((s) => (
            <SymptomChip key={s} label={s} active={s === situation} onPress={() => setSituation(s)} />
          ))}
        </View>

        <View style={styles.card}>
          <Text style={styles.cardTitle}>{situation} 응급처치</Text>
          {FIRST_AID_STEPS[situation].map((step, index) => (
            <View key={step} style={styles.stepRow}>
              <View style={styles.stepBadge}>
                <Text style={styles.stepBadgeText}>{index + 1}</Text>
              </View>
              <Text style={styles.stepText}>{step}</Text>
            </View>
          ))}
        </View>

        <Pressable style={styles.callBtn} onPress={callEmergency}>
          <Ionicons name="call" size={18} color={colors.white} />
          <Text style={styles.callBtnText}>119 연결</Text>
        </Pressable>
      </ScrollView>

      <DisclaimerFooter />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: { backgroundColor: colors.white, borderBottomWidth: 1, borderBottomColor: colors.border },
  headerRow: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
  },
  side: { width: 32, alignItems: 'center', justifyContent: 'center' },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  scrollContent: { padding: spacing.lg, gap: spacing.lg },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.inputBg,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm + 2,
  },
  searchInput: { flex: 1, fontSize: font.body, color: colors.text },
  chipRow: { flexDirection: 'row', gap: spacing.sm },
  card: {
    backgroundColor: colors.white,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
  cardTitle: { fontSize: font.body, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  stepRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.md },
  stepBadge: {
    width: 22,
    height: 22,
    borderRadius: radius.pill,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  stepBadgeText: { color: colors.white, fontSize: font.caption, fontWeight: '700' },
  stepText: { flex: 1, fontSize: font.sub, color: colors.text },
  callBtn: {
    height: 52,
    borderRadius: radius.md,
    backgroundColor: colors.primary,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
  },
  callBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});
