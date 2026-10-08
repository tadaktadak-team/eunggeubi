import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { FirstAidGuideDto, FirstAidTopicDto, getFirstAidGuide, searchFirstAidGuide } from '../api/firstAid';
import { AiConsultationStackParamList } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;

// 후보 카드 이름: 짧은 이름 뒤 괄호에 원래 이름(예: "베인 상처 (열상)"). 급할 때 첫 단어만 훑어도
// 구분되게 일상어를 앞에 둔다. 둘이 같으면 한 번만 보여준다.
const candidateLabel = (c: FirstAidTopicDto) =>
  c.shortName && c.shortName !== c.topic ? `${c.shortName} (${c.topic})` : c.topic;
type GuideRoute = RouteProp<AiConsultationStackParamList, 'FirstAidGuide'>;

export default function FirstAidGuideScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<GuideRoute>();
  const insets = useSafeAreaInsets();

  const [searchText, setSearchText] = useState('');
  const [guide, setGuide] = useState<FirstAidGuideDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [candidates, setCandidates] = useState<FirstAidTopicDto[]>([]);
  // 후보 목록을 보고 있는지 - 후보를 골라 응급처치로 들어가도 candidates는 남겨둬서 목록으로 되돌아올 수 있게 한다.
  const [showList, setShowList] = useState(false);
  // 여러 후보 중 하나를 골라 들어온 상태 - 이때만 "다른 상황 보기"로 목록에 돌아갈 수 있다.
  const canGoBackToList = !showList && candidates.length > 1;

  // 후보 중엔 참고자료에 현장 응급처치 내용이 없는 주제(예: 치통 및 만성 통증)가 섞일 수 있다 - 그걸 고르면
  // 목록에서 빼고 남은 후보로 돌려보내서, 같은 걸 또 누르거나 빈 화면에 갇히지 않게 한다.
  const selectSituation = async (c: FirstAidTopicDto) => {
    setShowList(false);
    const ok = await loadGuide(c.topic, { silent: true });
    if (ok) return;
    const rest = candidates.filter((other) => other.topic !== c.topic);
    setCandidates(rest);
    setShowList(rest.length > 0);
    Alert.alert('안내', `'${candidateLabel(c)}'은(는) 제공할 수 있는 응급처치 정보가 없어요.${rest.length > 0 ? ' 다른 상황을 골라주세요.' : ''}`);
  };

  // 헤더 뒤로가기도 후보에서 골라 들어온 상태면 화면을 나가지 않고 목록으로 돌아간다.
  const onPressBack = () => (canGoBackToList ? setShowList(true) : navigation.goBack());

  // 성공 여부를 돌려준다. silent면 실패 안내를 호출부가 직접 띄운다.
  const loadGuide = useCallback(async (target: string, { silent = false } = {}) => {
    setLoading(true);
    try {
      const result = await getFirstAidGuide(target);
      setGuide(result);
      return true;
    } catch (e) {
      console.warn(e);
      // 이전 상황의 스텝이 남아있으면 지금 고른 상황과 내용이 안 맞게 보인다 - 비워서
      // "이 상황은 못 불러왔다"를 명확히 한다.
      setGuide(null);
      if (!silent) Alert.alert('오류', '응급처치 정보를 불러오지 못했어요.');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  // AI 상담의 "응급처치 바로보기"로 들어오면 그 상황을 바로 보여준다. 그냥 들어오면 검색부터 하게 비워둔다.
  useEffect(() => {
    if (params?.situation) loadGuide(params.situation);
  }, [params?.situation, loadGuide]);

  const onSearchSubmit = async () => {
    const trimmed = searchText.trim();
    if (!trimmed) return;

    setLoading(true);
    try {
      const { candidates: matched } = await searchFirstAidGuide(trimmed);
      if (matched.length === 1) {
        // 후보가 하나뿐이면 고를 게 없으니 바로 응급처치를 보여준다 - await 안 하면 아래 finally가
        // 가이드를 다 불러오기 전에 로딩 표시를 꺼버린다.
        setCandidates([]);
        setShowList(false);
        await loadGuide(matched[0].topic);
      } else if (matched.length > 1) {
        // 여러 개면 가이드 자리에 후보 목록을 띄워 사용자가 고르게 한다.
        setCandidates(matched);
        setShowList(true);
        setGuide(null);
      } else {
        Alert.alert('안내', '해당 상황을 찾지 못했어요. 다른 표현으로 검색해주세요.');
      }
    } catch (e) {
      console.error(e);
      Alert.alert('오류', '검색 중 문제가 발생했어요.');
    } finally {
      setLoading(false);
    }
  };

  return (
      <View style={styles.container}>
        <View style={[styles.header, { paddingTop: insets.top }]}>
          <View style={styles.headerRow}>
            <Pressable style={styles.side} onPress={onPressBack} hitSlop={8}>
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

          {loading ? (
              <View style={styles.loadingBox}>
                <ActivityIndicator color={colors.primary} />
                {/* 처음 조회하는 상황은 서버가 LLM으로 가이드를 새로 만드느라 몇 초 걸릴 수 있다 -
                    스피너만 있으면 멈춘 것처럼 보일 수 있어 안내 문구를 더한다. */}
                <Text style={styles.loadingText}>맞춤 응급처치 가이드를 만들고 있어요...</Text>
              </View>
          ) : showList ? (
              <View style={styles.card}>
                <Text style={styles.cardTitle}>어떤 상황인가요?</Text>
                {candidates.map((c) => (
                    <Pressable key={c.topic} style={styles.candidateRow} onPress={() => selectSituation(c)}>
                      <View style={styles.candidateText}>
                        {!!c.category && <Text style={styles.categoryChip}>{c.category}</Text>}
                        <Text style={styles.candidateName}>{candidateLabel(c)}</Text>
                        {!!c.description && <Text style={styles.candidateDesc}>{c.description}</Text>}
                      </View>
                      <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
                    </Pressable>
                ))}
              </View>
          ) : (
              <>
                {canGoBackToList && (
                    <Pressable style={styles.backToListBtn} onPress={() => setShowList(true)} hitSlop={8}>
                      <Ionicons name="chevron-back" size={16} color={colors.textSub} />
                      <Text style={styles.backToListText}>다른 상황 보기</Text>
                    </Pressable>
                )}
                {guide && (
                    <View style={styles.card}>
                      <Text style={styles.cardTitle}>{guide.title}</Text>
                      {guide.steps.map((step, index) => (
                          <View key={step} style={styles.stepRow}>
                            <Text style={styles.stepMarker}>{guide.ordered ? `${index + 1}.` : '•'}</Text>
                            <Text style={styles.stepText}>{step}</Text>
                          </View>
                      ))}
                    </View>
                )}
              </>
          )}
        </ScrollView>

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
  // 두 줄 넘는 단계에서도 번호·점이 첫 줄 옆에 붙도록 위쪽 정렬.
  stepRow: { flexDirection: 'row', alignItems: 'flex-start', gap: spacing.sm, marginBottom: spacing.md },
  stepMarker: { fontSize: font.sub, color: colors.text },
  stepText: { flex: 1, fontSize: font.sub, color: colors.text },
  backToListBtn: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', gap: 2 },
  backToListText: { fontSize: font.sub, color: colors.textSub },
  candidateRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingVertical: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  candidateText: { flex: 1, alignItems: 'flex-start', gap: spacing.xs },
  categoryChip: {
    fontSize: font.caption,
    color: colors.textSub,
    backgroundColor: colors.inputBg,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    overflow: 'hidden',
  },
  candidateName: { fontSize: font.body, fontWeight: '700', color: colors.text },
  candidateDesc: { fontSize: font.sub, color: colors.textSub },
});