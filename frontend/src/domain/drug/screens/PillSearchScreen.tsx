import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { Ionicons, Feather } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { searchPills, PillSearchResponse } from '../api/pill';
import { addRecentSearch } from '../storage/recentSearches';
import DrugImage from '../components/DrugImage';

const NUM_OF_ROWS = 20;

// 모양/색상 칩. 식약처 낱알식별 데이터에 실제로 존재하는 값 전체를 빠짐없이 둔다(없으면 그 값을
// 가진 약은 낱알 특징으로는 찾을 수 없다). 색상은 "노랑, 투명"처럼 여러 색이 한 값에 들어있는
// 약도 있어 서버가 포함(LIKE) 방식으로 찾는다.
const SHAPES = [
  '원형', '타원형', '장방형', '반원형', '삼각형', '사각형', '마름모형', '오각형', '육각형', '팔각형', '기타',
];
const COLORS = [
  '하양', '노랑', '주황', '분홍', '빨강', '갈색', '연두', '초록', '청록', '파랑', '남색', '자주', '보라', '회색', '검정', '투명',
];

// 스크롤 중 같은 페이지가 두 번 붙어도 FlatList key(itemSeq)가 중복되지 않도록 걸러냄
const dedupeByItemSeq = (items: PillSearchResponse[]) => {
  const seen = new Set<string>();
  return items.filter((d) => {
    if (seen.has(d.itemSeq)) return false;
    seen.add(d.itemSeq);
    return true;
  });
};

const PillSearchScreen = ({ navigation }: any) => {
  const [printText, setPrintText] = useState('');
  const [selectedShape, setSelectedShape] = useState('');
  const [selectedColor, setSelectedColor] = useState('');

  const [results, setResults] = useState<PillSearchResponse[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [pageNo, setPageNo] = useState(1);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [searched, setSearched] = useState(false);

  // 다음 페이지를 이어 받을 때는 검색 버튼을 누른 시점의 조건을 그대로 써야 한다(그 사이 사용자가
  // 칩을 바꿔도 이미 보여준 결과와 섞이면 안 됨). 늦게 도착한 이전 응답은 requestId로 버린다.
  const appliedQueryRef = useRef<{ drugShape?: string; colorClass?: string; imprint?: string }>({});
  const fetchingMoreRef = useRef(false);
  const requestIdRef = useRef(0);
  // 새 검색이 끝났을 때만 결과 영역으로 자동 스크롤한다(추가 페이지를 붙일 때는 스크롤하지 않음).
  const shouldScrollRef = useRef(false);

  // 필터(식별문자/모양/색상)가 길어서, 검색 버튼을 눌러도 결과가 화면 아래쪽에 있어 바로 안
  // 보이고 직접 스크롤해야 하는 문제가 있었다. 검색이 끝나면 필터 영역 높이만큼 자동으로 스크롤해
  // 결과(또는 "결과 없음" 안내)가 바로 보이게 한다.
  const listRef = useRef<FlatList<PillSearchResponse>>(null);
  const headerHeightRef = useRef(0);

  const handleReset = () => {
    setPrintText('');
    setSelectedShape('');
    setSelectedColor('');
  };

  const handleSearch = async () => {
    if (!printText.trim() && !selectedShape && !selectedColor) {
      Alert.alert('조건 필요', '모양, 색상, 식별문자 중 하나는 입력해주세요.');
      return;
    }
    requestIdRef.current += 1;
    const requestId = requestIdRef.current;
    const query = {
      drugShape: selectedShape || undefined,
      colorClass: selectedColor || undefined,
      imprint: printText.trim() || undefined,
    };
    try {
      setLoading(true);
      setSearched(true);
      const data = await searchPills({ ...query, pageNo: 1, numOfRows: NUM_OF_ROWS });
      if (requestId !== requestIdRef.current) return; // 그 사이 더 새로운 검색이 시작됐으면 버림
      appliedQueryRef.current = query;
      setResults(dedupeByItemSeq(data.items));
      setTotalCount(data.totalCount);
      setPageNo(1);
      shouldScrollRef.current = true;
    } catch (error) {
      console.error('낱알 특징 검색 오류:', error);
      Alert.alert('검색 실패', '검색 중 오류가 발생했습니다. 서버 연결 상태를 확인해 주세요.');
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  };

  // 스크롤이 끝에 닿으면 다음 페이지를 이어붙임
  const loadMore = async () => {
    if (loading || fetchingMoreRef.current) return;
    if (results.length === 0 || pageNo * NUM_OF_ROWS >= totalCount) return;

    const requestId = requestIdRef.current;
    const nextPage = pageNo + 1;
    fetchingMoreRef.current = true;
    try {
      setLoadingMore(true);
      const data = await searchPills({ ...appliedQueryRef.current, pageNo: nextPage, numOfRows: NUM_OF_ROWS });
      if (requestId !== requestIdRef.current) return;
      setResults((prev) => dedupeByItemSeq([...prev, ...data.items]));
      setTotalCount(data.totalCount);
      setPageNo(nextPage);
    } catch (error) {
      console.error('낱알 특징 검색 추가 로딩 오류:', error);
      Alert.alert('불러오기 실패', '추가 결과를 불러오지 못했습니다. 다시 스크롤해 재시도해 주세요.');
    } finally {
      fetchingMoreRef.current = false;
      setLoadingMore(false);
    }
  };

  // loading이 false로 바뀌어 헤더가 최종 상태("검색 결과 (N)")로 다시 그려진 뒤에 스크롤해야
  // 해서, onLayout으로 측정된 높이를 약간의 지연 후에 사용한다(레이아웃이 먼저 안정되도록).
  useEffect(() => {
    if (!searched || loading || !shouldScrollRef.current) return;
    shouldScrollRef.current = false;
    const timer = setTimeout(() => {
      listRef.current?.scrollToOffset({ offset: headerHeightRef.current, animated: true });
    }, 50);
    return () => clearTimeout(timer);
  }, [results, searched, loading]);

  // 필터 UI(식별문자/모양/색상)는 결과와 달리 몇 개 안 되는 고정 항목이라 그냥 렌더링해도
  // 되지만, 결과 카드는 조건에 따라 수천 건까지 나올 수 있어(예: 원형+하양 4,944건) ScrollView에
  // 전부 올려두면 전부 한 번에 네이티브 뷰로 그려져 렉이 심했다. FlatList로 바꿔 화면에 보이는
  // 만큼만 그리도록 하고, 필터 UI는 FlatList의 ListHeaderComponent로 넣어 하나의 스크롤로 유지한다.
  const renderFilters = () => (
    <View onLayout={(e) => { headerHeightRef.current = e.nativeEvent.layout.height; }}>
      <View style={styles.resetRow}>
        <TouchableOpacity onPress={handleReset} hitSlop={8}>
          <Text style={styles.resetText}>초기화</Text>
        </TouchableOpacity>
      </View>

      {/* 식별문자 입력 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>식별문자</Text>
        <TextInput
          style={styles.textInput}
          placeholder="알약에 적힌 글자 (예: TY, 500)"
          value={printText}
          onChangeText={setPrintText}
          placeholderTextColor={colors.placeholder}
        />
      </View>

      {/* 모양 선택 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>모양</Text>
        <View style={styles.chipContainer}>
          {SHAPES.map((shape) => (
            <TouchableOpacity
              key={shape}
              style={[styles.chip, selectedShape === shape && styles.chipSelected]}
              onPress={() => setSelectedShape(selectedShape === shape ? '' : shape)}
            >
              <Text style={[styles.chipText, selectedShape === shape && styles.chipTextSelected]}>
                {shape}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* 색상 선택 */}
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>색상</Text>
        <View style={styles.chipContainer}>
          {COLORS.map((color) => (
            <TouchableOpacity
              key={color}
              style={[styles.chip, selectedColor === color && styles.chipSelected]}
              onPress={() => setSelectedColor(selectedColor === color ? '' : color)}
            >
              <Text style={[styles.chipText, selectedColor === color && styles.chipTextSelected]}>
                {color}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {loading && (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
        </View>
      )}

      {!loading && searched && (
        <Text style={[styles.sectionTitle, { marginBottom: spacing.md }]}>검색 결과 ({totalCount.toLocaleString()})</Text>
      )}
    </View>
  );

  return (
    <View style={styles.container}>
      <AppHeader title="낱알 특징 검색" />

      <FlatList
        ref={listRef}
        contentContainerStyle={styles.scrollContent}
        data={results}
        keyExtractor={(item) => item.itemSeq}
        ListHeaderComponent={renderFilters}
        ListEmptyComponent={
          !loading && searched ? <Text style={styles.emptyText}>조건에 맞는 약을 찾지 못했습니다.</Text> : null
        }
        onEndReached={loadMore}
        onEndReachedThreshold={0.4}
        ListFooterComponent={
          loadingMore ? <ActivityIndicator size="small" color={colors.primary} style={styles.footerLoading} /> : null
        }
        // 결과가 수천 건까지 나올 수 있어 초기/배치 렌더링 양을 보수적으로 잡고 화면 밖 항목은
        // 뷰 자체를 비워(removeClippedSubviews) 메모리·렉을 줄인다.
        initialNumToRender={12}
        maxToRenderPerBatch={12}
        windowSize={7}
        removeClippedSubviews
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.resultCard}
            onPress={() => {
              addRecentSearch({
                itemSeq: item.itemSeq,
                name: item.itemName,
                drugType: item.etcOtcName,
                itemImage: item.itemImage,
              });
              navigation.navigate('DrugDetail', { itemSeq: item.itemSeq });
            }}
          >
            <DrugImage uri={item.itemImage} name={item.itemName} size={56} style={styles.resultImage} />
            <View style={styles.resultInfo}>
              <Text style={styles.resultName} numberOfLines={1}>
                {item.itemName}
              </Text>
              {item.entpName && (
                <Text style={styles.resultEntp} numberOfLines={1}>
                  {item.entpName}
                </Text>
              )}
            </View>
            <Feather name="chevron-right" size={20} color={colors.placeholder} />
          </TouchableOpacity>
        )}
      />

      {/* 하단 검색하기 버튼 */}
      <View style={styles.bottomContainer}>
        <TouchableOpacity style={styles.searchButton} activeOpacity={0.8} onPress={handleSearch}>
          <Ionicons name="search" size={20} color={colors.white} style={{ marginRight: spacing.sm }} />
          <Text style={styles.searchButtonText}>조건으로 약물 검색</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  resetRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginBottom: spacing.md,
  },
  resetText: {
    fontSize: font.sub,
    color: colors.textSub,
    fontWeight: '600',
  },
  scrollContent: {
    padding: spacing.xl,
    paddingBottom: spacing.xxl + spacing.md,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: spacing.md,
  },
  textInput: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 48,
    fontSize: font.body,
    color: colors.text,
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm + 2,
    borderRadius: radius.pill,
    backgroundColor: colors.inputBg,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  chipSelected: {
    backgroundColor: colors.primaryLight,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: font.sub,
    color: colors.textSub,
  },
  chipTextSelected: {
    color: colors.primaryDark,
    fontWeight: 'bold',
  },
  bottomContainer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.white,
  },
  searchButton: {
    backgroundColor: colors.black,
    borderRadius: radius.md,
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchButtonText: {
    color: colors.white,
    fontSize: font.body,
    fontWeight: 'bold',
  },
  centerContainer: {
    alignItems: 'center',
    paddingVertical: spacing.xl,
  },
  emptyText: {
    fontSize: font.body,
    color: colors.textSub,
  },
  resultCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md + 2,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  resultImage: {
    marginRight: spacing.md,
  },
  footerLoading: {
    marginVertical: spacing.lg,
  },
  resultInfo: {
    flex: 1,
  },
  resultName: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: 2,
  },
  resultEntp: {
    fontSize: font.sub,
    color: colors.textSub,
  },
});

export default PillSearchScreen;
