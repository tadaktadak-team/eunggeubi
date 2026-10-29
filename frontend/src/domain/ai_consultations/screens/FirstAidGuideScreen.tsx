import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { showAlert } from '../../../shared/dialog';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import {
  FirstAidGuideDto,
  FirstAidTopicDto,
  getFirstAidTopics,
  searchFirstAidGuide,
  streamFirstAidGuide,
} from '../api/firstAid';
import { AiConsultationStackParamList } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;

// 후보 카드 이름: 짧은 이름 뒤 괄호에 원래 이름(예: "베인 상처 (열상)"). 급할 때 첫 단어만 훑어도
// 구분되게 일상어를 앞에 둔다. 둘이 같으면 한 번만 보여준다.
const candidateLabel = (c: FirstAidTopicDto) =>
  c.shortName && c.shortName !== c.topic ? `${c.shortName} (${c.topic})` : c.topic;

// 처음 들어왔을 때 검색 없이 바로 누르는 자주 찾는 상황. topic은 서버 first-aid-topics.txt의 주제 이름과
// 글자까지 같아야 한다(다르면 가이드를 못 찾는다). 아이콘은 Ionicons에 없는 뼈·머리가 있는 MaterialCommunityIcons.
const QUICK_TOPICS: { topic: string; label: string; icon: keyof typeof MaterialCommunityIcons.glyphMap }[] = [
  { topic: '화상', label: '화상', icon: 'fire' },
  { topic: '코피(비출혈)', label: '코피', icon: 'water' },
  { topic: '열상', label: '베인 상처', icon: 'content-cut' },
  { topic: '골절', label: '골절', icon: 'bone' },
  { topic: '염좌', label: '삠', icon: 'walk' },
  { topic: '외상성 뇌손상', label: '머리 부딪힘', icon: 'head-flash' },
  { topic: '동물 및 곤충에 의한 응급상황', label: '동물·벌레 물림', icon: 'bug' },
  { topic: '안외상(각막이물)', label: '눈에 이물질', icon: 'eye' },
];
// first-aid-topics.txt의 [안전] 분류 이름.
const SAFETY_CATEGORY = '안전';
type GuideRoute =RouteProp<AiConsultationStackParamList, 'FirstAidGuide'>;

export default function FirstAidGuideScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<GuideRoute>();
  const insets = useSafeAreaInsets();

  const [searchText, setSearchText] = useState('');
  const [guide, setGuide] = useState<FirstAidGuideDto | null>(null);
  const [loading, setLoading] = useState(false);
  // 단계가 스트리밍으로 채워지는 중 - 첫 단계가 오면 로딩 표시 대신 카드를 띄우고 아래에 작은 스피너를 둔다.
  const [generating, setGenerating] = useState(false);
  // 가장 최근 조회 번호. 스트림이 도는 중에 다른 상황으로 넘어가면 이전 스트림의 단계가 덮어쓰지 않게 한다.
  const requestRef = useRef(0);
  const [candidates, setCandidates] = useState<FirstAidTopicDto[]>([]);
  // 후보 목록을 보고 있는지 - 후보를 골라 응급처치로 들어가도 candidates는 남겨둬서 목록으로 되돌아올 수 있게 한다.
  const [showList, setShowList] = useState(false);
  // 여러 후보 중 하나를 골라 들어온 상태 - 이때만 "다른 상황 보기"로 목록에 돌아갈 수 있다.
  const canGoBackToList = !showList && candidates.length > 1;
  // 원인별로 처치가 다른 주제(예: 동물·벌레 물림)를 고르면 서버가 세부 상황 목록(cases)을 준다 - 하나를 고르면
  // 그 경우의 응급처치만 불러온다. 고른 뒤에도 남겨둬서 "다른 경우 보기"로 되돌아올 수 있게 한다.
  const [caseMenu, setCaseMenu] = useState<{ situation: string; title: string; cases: string[] } | null>(null);
  const viewingCase = !!caseMenu && !!guide;

  // 후보 중엔 참고자료에 현장 응급처치 내용이 없는 주제(예: 치통 및 만성 통증)가 섞일 수 있다 - 그걸 고르면
  // 목록에서 빼고 남은 후보로 돌려보내서, 같은 걸 또 누르거나 빈 화면에 갇히지 않게 한다.
  const selectSituation = async (c: FirstAidTopicDto) => {
    setShowList(false);
    const ok = await loadGuide(c.topic, { silent: true });
    if (ok) return;
    const rest = candidates.filter((other) => other.topic !== c.topic);
    setCandidates(rest);
    setShowList(rest.length > 0);
    showAlert('안내', `'${candidateLabel(c)}'은(는) 제공할 수 있는 응급처치 정보가 없어요.${rest.length > 0 ? ' 다른 상황을 골라주세요.' : ''}`);
  };

  // 직접 검색·자주 찾는 상황으로 연 가이드면 처음 화면(자주 찾는 상황)으로 돌아갈 수 있다.
  // AI 상담에서 바로 들어온 경우는 돌아갈 처음 화면이 없으니 뺀다.
  const canGoBackHome = !!(guide || caseMenu) && !params?.situation;
  const goBackHome = () => {
    requestRef.current++;
    setGuide(null);
    setCaseMenu(null);
  };

  // 헤더 뒤로가기도 세부 상황이나 후보에서 골라 들어온 상태면 화면을 나가지 않고 그 목록으로 돌아간다.
  // 검색 후보 목록을 보고 있으면 화면을 나가지 않고 처음 화면(자주 찾는 상황)으로 돌아간다.
  // 후보에서 골라 들어갔던 세부 상황 목록(caseMenu)·가이드도 같이 비운다 - 남아 있으면 처음 화면 대신 그게 보인다.
  const closeList = () => {
    setShowList(false);
    setCandidates([]);
    goBackHome();
  };
  const onPressBack = () => {
    if (showList) closeList();
    else if (viewingCase) setGuide(null);
    else if (canGoBackToList) setShowList(true);
    else if (canGoBackHome) goBackHome();
    else navigation.goBack();
  };

  // 성공 여부를 돌려준다. silent면 실패 안내를 호출부가 직접 띄운다. caseName은 세부 상황을 고른 경우에만 준다.
  const loadGuide = useCallback(async (target: string, { silent = false, caseName }: { silent?: boolean; caseName?: string } = {}) => {
    const req = ++requestRef.current;
    const isCurrent = () => requestRef.current === req;
    setLoading(true);
    try {
      const result = await streamFirstAidGuide(target, caseName, (partial) => {
        if (!isCurrent()) return;
        if (!caseName) setCaseMenu(null);
        setGuide(partial);
        setLoading(false);
        setGenerating(true);
      });
      if (!isCurrent()) return true;
      if (result.cases?.length) {
        // 세부 상황이 있는 주제 - 단계 대신 고를 목록을 보여준다.
        setCaseMenu({ situation: target, title: result.title, cases: result.cases });
        setGuide(null);
      } else {
        // 다른 주제로 넘어왔으면 이전 주제의 세부 상황 목록은 버린다.
        if (!caseName) setCaseMenu(null);
        setGuide(result);
      }
      return true;
    } catch (e) {
      console.warn(e);
      if (!isCurrent()) return true;
      // 이전 상황의 스텝이 남아있으면 지금 고른 상황과 내용이 안 맞게 보인다 - 비워서
      // "이 상황은 못 불러왔다"를 명확히 한다.
      setGuide(null);
      if (!silent) showAlert('오류', '응급처치 정보를 불러오지 못했어요.');
      return false;
    } finally {
      if (isCurrent()) {
        setLoading(false);
        setGenerating(false);
      }
    }
  }, []);

  // AI 상담의 "응급처치 바로보기"로 들어오면 그 상황을 바로 보여준다. 그냥 들어오면 검색부터 하게 비워둔다.
  useEffect(() => {
    if (params?.situation) loadGuide(params.situation);
  }, [params?.situation, loadGuide]);

  // "상황별로 찾기" 목록. 못 불러와도 검색·자주 찾는 상황은 쓸 수 있으니 목록만 안 보여준다.
  const [topics, setTopics] = useState<FirstAidTopicDto[]>([]);
  const [openCategory, setOpenCategory] = useState<string | null>(null);
  useEffect(() => {
    getFirstAidTopics().then((r) => setTopics(r.candidates)).catch(console.warn);
  }, []);
  // 성폭력·아동학대는 응급처치와 결이 달라 분류 목록과 따로 보여준다.
  const safetyTopics = topics.filter((t) => t.category === SAFETY_CATEGORY);
  const categories = [...new Set(topics.map((t) => t.category))].filter((c) => c !== SAFETY_CATEGORY);

  // 자주 찾는 상황·상황별로 찾기에서 주제를 바로 연다. 이전 검색 후보가 남으면 "다른 상황 보기"가 엉뚱한 목록을 띄운다.
  const openTopic = (topic: string) => {
    setCandidates([]);
    loadGuide(topic);
  };

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
        requestRef.current++;
        setCandidates(matched);
        setShowList(true);
        setGuide(null);
        setCaseMenu(null);
      } else {
        showAlert('안내', '해당 상황을 찾지 못했어요. 다른 표현으로 검색해주세요.');
      }
    } catch (e) {
      console.error(e);
      showAlert('오류', '검색 중 문제가 발생했어요.');
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
            <TextInput
                style={styles.searchInput}
                placeholder="상황 입력 (예: 화상, 코피)"
                placeholderTextColor={colors.placeholder}
                value={searchText}
                onChangeText={setSearchText}
                onSubmitEditing={onSearchSubmit}
                returnKeyType="search"
            />
            <Pressable onPress={onSearchSubmit} hitSlop={8} accessibilityLabel="검색">
              <Feather name="search" size={18} color={colors.placeholder} />
            </Pressable>
          </View>

          {loading ? (
              <View style={styles.loadingBox}>
                <ActivityIndicator color={colors.primary} />
                {/* 처음 조회하는 상황은 서버가 LLM으로 가이드를 새로 만드느라 몇 초 걸릴 수 있다 -
                    스피너만 있으면 멈춘 것처럼 보일 수 있어 안내 문구를 더한다. */}
                <Text style={styles.loadingText}>맞춤 응급처치 가이드를 만들고 있어요...</Text>
              </View>
          ) : showList ? (
              <>
              {/* 검색 결과(후보 목록)에서 처음 화면으로 돌아가는 아이콘 버튼 - 가이드 화면의 것과 같은 모양. */}
              <Pressable style={styles.backToListBtn} onPress={closeList} hitSlop={8} accessibilityLabel="처음 화면으로">
                <Ionicons name="chevron-back" size={16} color={colors.textSub} />
              </Pressable>
              <View style={styles.card}>
                <Text style={styles.cardTitle}>어떤 상황인가요?</Text>
                {candidates.map((c) => (
                    <Pressable key={c.topic} style={styles.candidateRow} onPress={() => selectSituation(c)}>
                      <View style={styles.candidateText}>
                        <Text style={styles.candidateName}>{candidateLabel(c)}</Text>
                        {!!c.description && <Text style={styles.candidateDesc}>{c.description}</Text>}
                      </View>
                      <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
                    </Pressable>
                ))}
              </View>
              </>
          ) : (
              <>
                {viewingCase ? (
                    <Pressable style={styles.backToListBtn} onPress={() => setGuide(null)} hitSlop={8}>
                      <Ionicons name="chevron-back" size={16} color={colors.textSub} />
                      <Text style={styles.backToListText}>다른 경우 보기</Text>
                    </Pressable>
                ) : (canGoBackToList || canGoBackHome) && (
                    // 후보 목록(여러 후보에서 골랐을 때)이나 처음 화면(자주 찾는 상황)으로 돌아가는 아이콘 버튼.
                    <Pressable
                        style={styles.backToListBtn}
                        onPress={canGoBackToList ? () => setShowList(true) : goBackHome}
                        hitSlop={8}
                        accessibilityLabel="다른 상황 보기"
                    >
                      <Ionicons name="chevron-back" size={16} color={colors.textSub} />
                    </Pressable>
                )}
                {!guide && caseMenu && (
                    <View style={styles.card}>
                      <Text style={styles.cardTitle}>{caseMenu.title}</Text>
                      <Text style={styles.caseQuestion}>어떤 경우인가요?</Text>
                      {caseMenu.cases.map((c) => (
                          <Pressable
                              key={c}
                              style={styles.candidateRow}
                              onPress={() => loadGuide(caseMenu.situation, { caseName: c })}
                          >
                            <Text style={[styles.candidateName, styles.candidateText]}>{c}</Text>
                            <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
                          </Pressable>
                      ))}
                    </View>
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
                      {generating && <ActivityIndicator color={colors.primary} style={styles.stepSpinner} />}
                    </View>
                )}
                {!guide && !caseMenu && (
                    <>
                      <View style={styles.card}>
                        <Text style={styles.cardTitle}>자주 찾는 상황</Text>
                        <View style={styles.quickGrid}>
                          {QUICK_TOPICS.map((t) => (
                              <Pressable key={t.topic} style={styles.quickItem} onPress={() => openTopic(t.topic)}>
                                <MaterialCommunityIcons name={t.icon} size={24} color={colors.primary} />
                                <Text style={styles.quickLabel}>{t.label}</Text>
                              </Pressable>
                          ))}
                        </View>
                      </View>
                      {categories.length > 0 && (
                          <View style={styles.card}>
                            <Text style={styles.cardTitle}>상황별로 찾기</Text>
                            {categories.map((category) => {
                              const items = topics.filter((t) => t.category === category);
                              const open = openCategory === category;
                              return (
                                  <View key={category}>
                                    <Pressable
                                        style={styles.candidateRow}
                                        onPress={() => setOpenCategory(open ? null : category)}
                                    >
                                      <Text style={[styles.candidateName, styles.candidateText]}>
                                        {category} <Text style={styles.categoryCount}>({items.length})</Text>
                                      </Text>
                                      <Ionicons name={open ? 'chevron-up' : 'chevron-down'} size={18} color={colors.placeholder} />
                                    </Pressable>
                                    {open && items.map((t) => (
                                        <TopicRow key={t.topic} topic={t} onPress={() => openTopic(t.topic)} />
                                    ))}
                                  </View>
                              );
                            })}
                          </View>
                      )}
                      {safetyTopics.length > 0 && (
                          <View style={styles.card}>
                            <Text style={styles.cardTitle}>도움이 필요할 때</Text>
                            {safetyTopics.map((t) => (
                                <TopicRow key={t.topic} topic={t} onPress={() => openTopic(t.topic)} />
                            ))}
                          </View>
                      )}
                    </>
                )}
                {/* 응급처치 단계는 서버가 질병관리청 국가건강정보포털 자료만 근거로 만든다(FirstAidPrompts). */}
                {guide && <Text style={styles.sourceFooter}>출처: 질병관리청 국가건강정보포털</Text>}
              </>
          )}
        </ScrollView>

      </View>
  );
}

// 상황별로 찾기·도움이 필요할 때의 주제 한 줄 - 짧은 이름과 "이럴 때 열어 보세요" 설명.
function TopicRow({ topic, onPress }: { topic: FirstAidTopicDto; onPress: () => void }) {
  return (
      <Pressable style={[styles.candidateRow, styles.topicRow]} onPress={onPress}>
        <View style={styles.candidateText}>
          <Text style={styles.topicName}>{topic.shortName || topic.topic}</Text>
          {!!topic.description && <Text style={styles.candidateDesc}>{topic.description}</Text>}
        </View>
        <Ionicons name="chevron-forward" size={18} color={colors.placeholder} />
      </Pressable>
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
  // 약물정보 화면 검색창(DrugHomeScreen searchContainer)과 같은 높이·모서리·아이콘으로 맞춤.
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.inputBg,
    borderRadius: 10,
    paddingHorizontal: spacing.lg,
    height: 48,
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
  stepSpinner: { alignSelf: 'flex-start' },
  backToListBtn: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', gap: 2 },
  backToListText: { fontSize: font.sub, color: colors.textSub },
  sourceFooter: { textAlign: 'center', color: colors.placeholder, fontSize: font.caption },
  candidateRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingVertical: spacing.md,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  candidateText: { flex: 1, alignItems: 'flex-start', gap: spacing.xs },
  candidateName: { fontSize: font.body, fontWeight: '700', color: colors.text },
  candidateDesc: { fontSize: font.sub, color: colors.textSub },
  // 4열 2줄. 라벨이 긴 칸(동물·벌레 물림)은 두 줄로 접힌다.
  quickGrid: { flexDirection: 'row', flexWrap: 'wrap', rowGap: spacing.md },
  quickItem: { width: '25%', alignItems: 'center', gap: spacing.xs },
  quickLabel: { fontSize: font.sub, color: colors.text, textAlign: 'center' },
  categoryCount: { fontWeight: '400', color: colors.placeholder },
  // 펼친 분류 안의 주제는 들여써서 분류 줄과 구분한다.
  topicRow: { paddingLeft: spacing.md },
  topicName: { fontSize: font.sub, fontWeight: '700', color: colors.text },
  caseQuestion: { fontSize: font.sub, color: colors.textSub, marginBottom: spacing.sm },
});