import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Linking, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { FirstAidGuideDto, getFirstAidGuide, searchFirstAidGuide } from '../api/firstAid';
import DisclaimerFooter from '../components/DisclaimerFooter';
import SymptomChip from '../components/SymptomChip';
import { AiConsultationStackParamList, FIRST_AID_SITUATIONS } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;
type GuideRoute = RouteProp<AiConsultationStackParamList, 'FirstAidGuide'>;

export default function FirstAidGuideScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<GuideRoute>();
  const insets = useSafeAreaInsets();

  const [searchText, setSearchText] = useState('');
  const [situation, setSituation] = useState<string>(params?.situation ?? FIRST_AID_SITUATIONS[0]);
  const [guide, setGuide] = useState<FirstAidGuideDto | null>(null);
  const [loading, setLoading] = useState(false);

  // 선택된 상황(situation)이 바뀔 때마다 서버에서 해당 응급처치 단계를 조회
  const loadGuide = useCallback(async (target: string) => {
    setLoading(true);
    try {
      const result = await getFirstAidGuide(target);
      setGuide(result);
    } catch (e) {
      console.error(e);
      // 이전 상황의 스텝이 남아있으면 지금 선택된 칩/situation과 내용이 안 맞게 보인다 - 비워서
      // "이 상황은 못 불러왔다"를 명확히 한다.
      setGuide(null);
      Alert.alert('오류', '응급처치 정보를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadGuide(situation);
  }, [situation, loadGuide]);

  const onSearchSubmit = async () => {
    const trimmed = searchText.trim();
    if (!trimmed) return;

    setLoading(true);
    try {
      const result = await searchFirstAidGuide(trimmed);
      if (result) {
        setSituation(result.situation);
        setGuide(result);
      } else {
        Alert.alert('안내', '해당 상황을 찾지 못했어요. 아래 목록에서 골라주세요.');
      }
    } catch (e) {
      console.error(e);
      Alert.alert('오류', '검색 중 문제가 발생했어요.');
    } finally {
      setLoading(false);
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

          {loading ? (
              <View style={styles.loadingBox}>
                <ActivityIndicator color={colors.primary} />
                {/* 처음 조회하는 상황은 서버가 LLM으로 가이드를 새로 만드느라 몇 초 걸릴 수 있다 -
                    스피너만 있으면 멈춘 것처럼 보일 수 있어 안내 문구를 더한다. */}
                <Text style={styles.loadingText}>맞춤 응급처치 가이드를 만들고 있어요...</Text>
              </View>
          ) : (
              guide && (
                  <View style={styles.card}>
                    <Text style={styles.cardTitle}>{guide.title}</Text>
                    {guide.steps.map((step, index) => (
                        <View key={step} style={styles.stepRow}>
                          <View style={styles.stepBadge}>
                            <Text style={styles.stepBadgeText}>{index + 1}</Text>
                          </View>
                          <Text style={styles.stepText}>{step}</Text>
                        </View>
                    ))}
                  </View>
              )
          )}

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
  loadingBox: { alignItems: 'center', gap: spacing.sm, marginVertical: spacing.xl },
  loadingText: { fontSize: font.sub, color: colors.placeholder },
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