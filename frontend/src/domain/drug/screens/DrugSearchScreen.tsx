import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  StyleSheet,
  Image,
  Alert,
} from 'react-native';
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { searchDrugsByName, DrugInfoResponse } from '../api/drug';
import { addRecentSearch } from '../storage/recentSearches';
import { stripHtmlTags } from '../../../shared/utils/html';
import { getDrugFormIconName } from '../utils/drugIcon';
import { getPermitStatusLabel } from '../utils/drugStatus';

const NUM_OF_ROWS = 10;

// 스크롤 중 같은 페이지가 두 번 붙거나 페이지 경계에서 같은 약이 겹쳐 내려와도
// FlatList key(itemSeq)가 중복되지 않도록 itemSeq 기준으로 걸러냄
const dedupeByItemSeq = (items: DrugInfoResponse[]) => {
  const seen = new Set<string>();
  return items.filter((d) => {
    if (seen.has(d.itemSeq)) return false;
    seen.add(d.itemSeq);
    return true;
  });
};

const DrugSearchScreen: React.FC<{ navigation: any; route: any }> = ({ navigation, route }) => {
  const initialKeyword: string = route?.params?.initialKeyword ?? '';
  // 상호작용 체크 화면 등에서 "약 고르기" 용도로 이 화면을 열었을 때 켜는 모드.
  // 켜져 있으면 결과 탭 시 상세화면으로 가는 대신 고른 약을 onSelect로 돌려주고 뒤로 간다.
  const selectMode: boolean = route?.params?.selectMode ?? false;
  const onSelect: ((drug: DrugInfoResponse) => void) | undefined = route?.params?.onSelect;

  const [keyword, setKeyword] = useState(initialKeyword);
  const [searchedKeyword, setSearchedKeyword] = useState('');
  const [drugs, setDrugs] = useState<DrugInfoResponse[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [searched, setSearched] = useState(false);

  // state는 비동기로 갱신돼서 onEndReached가 연달아 호출되면 loadingMore 검사를 둘 다 통과
  // ref로 즉시 잠가서 같은 페이지를 두 번 요청하지 않게 하고, 새 검색이 시작되면 이전 요청 결과는 버림
  const fetchingMoreRef = useRef(false);
  const requestIdRef = useRef(0);

  // 새 검색어로 첫 페이지부터 다시 검색
  const runSearch = async (kw: string) => {
    if (!kw.trim()) return;
    requestIdRef.current += 1;
    const requestId = requestIdRef.current;
    try {
      setLoading(true);
      setSearched(true);
      setSearchedKeyword(kw.trim());

      const result = await searchDrugsByName(kw.trim(), 1, NUM_OF_ROWS);
      if (requestId !== requestIdRef.current) return; // 그 사이 더 새로운 검색이 시작됐으면 이 응답은 버림
      setDrugs(dedupeByItemSeq(result.items));
      setPageNo(1);
      setTotalCount(result.totalCount);
    } catch (error) {
      console.error('약품 검색 오류:', error);
      Alert.alert('검색 실패', '검색 중 오류가 발생했습니다. 서버 연결 상태를 확인해 주세요.');
      if (requestId !== requestIdRef.current) return; // 실패도 최신 요청 건일 때만 반영
      setDrugs([]);
      setTotalCount(0);
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  };

  // 스크롤이 끝에 닿으면 다음 페이지를 이어붙임
  const loadMore = async () => {
    if (loading || fetchingMoreRef.current) return;
    // 중복 제거로 목록 길이가 줄 수 있어서 길이가 아니라 페이지 수로 끝을 판단한다
    if (pageNo * NUM_OF_ROWS >= totalCount) return;

    const requestId = requestIdRef.current;
    const nextPage = pageNo + 1;
    fetchingMoreRef.current = true;
    try {
      setLoadingMore(true);
      const result = await searchDrugsByName(searchedKeyword, nextPage, NUM_OF_ROWS);
      if (requestId !== requestIdRef.current) return;
      setDrugs((prev) => dedupeByItemSeq([...prev, ...result.items]));
      setPageNo(nextPage);
      setTotalCount(result.totalCount);
    } catch (error) {
      console.error('약품 검색 추가 로딩 오류:', error);
      // runSearch와 달리 여기서 Alert가 없으면, 실패로 조용히 멈춘 게 "결과 끝"처럼 보여서
      // 사용자가 재시도할 방법도 모른 채 더 있는 결과를 놓치게 된다.
      Alert.alert('불러오기 실패', '추가 결과를 불러오지 못했습니다. 다시 스크롤해 재시도해 주세요.');
    } finally {
      fetchingMoreRef.current = false;
      setLoadingMore(false);
    }
  };

  // 홈 화면에서 넘어온 검색어가 있으면 진입 시 한 번만 자동 검색
  useEffect(() => {
    if (initialKeyword.trim()) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- 진입 시 1회만 자동 검색, 무한루프/렌더링 문제 없음
      runSearch(initialKeyword);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => runSearch(keyword);

  return (
    <View style={styles.container}>
      <AppHeader title={selectMode ? '약 선택' : '약품명 검색'} />

      <View style={styles.searchContainer}>
        <View style={styles.searchRow}>
          <Feather name="search" size={18} color={colors.placeholder} style={styles.searchIcon} />
          <TextInput
            style={styles.searchInput}
            placeholder="약 이름을 입력하세요"
            placeholderTextColor={colors.placeholder}
            value={keyword}
            onChangeText={setKeyword}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
          />
          {keyword.length > 0 && (
            <TouchableOpacity onPress={() => setKeyword('')}>
              <Ionicons name="close-circle" size={18} color={colors.placeholder} />
            </TouchableOpacity>
          )}
        </View>
        <TouchableOpacity style={styles.searchBtn} onPress={handleSearch}>
          <Text style={styles.searchBtnText}>검색</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color={colors.primary} />
          <Text style={styles.loadingText}>약 정보를 불러오는 중...</Text>
        </View>
      ) : (
        <FlatList
          data={drugs}
          keyExtractor={(item) => item.itemSeq}
          contentContainerStyle={styles.listContent}
          onEndReached={loadMore}
          onEndReachedThreshold={0.4}
          ListFooterComponent={
            loadingMore ? (
              <ActivityIndicator size="small" color={colors.primary} style={styles.footerLoading} />
            ) : null
          }
          ListEmptyComponent={
            searched ? (
              <View style={styles.centerContainer}>
                <Text style={styles.emptyText}>검색 결과가 없습니다.</Text>
              </View>
            ) : (
              <View style={styles.centerContainer}>
                <Feather name="search" size={32} color={colors.border} />
                <Text style={styles.guideText}>궁금한 약의 이름을 검색해 보세요.</Text>
              </View>
            )
          }
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.card}
              onPress={() => {
                if (selectMode) {
                  // 상호작용 체크용으로 잠깐 고르는 것뿐이라 최근 검색엔 남기지 않는다.
                  onSelect?.(item);
                  navigation.goBack();
                  return;
                }
                addRecentSearch({
                  itemSeq: item.itemSeq,
                  name: item.name,
                  drugType: item.drugType,
                  itemImage: item.itemImage,
                });
                navigation.navigate('DrugDetail', { itemSeq: item.itemSeq });
              }}
            >
              {item.itemImage ? (
                <Image source={{ uri: item.itemImage }} style={styles.drugImage} />
              ) : (
                <View style={styles.noImage}>
                  <MaterialCommunityIcons
                    name={getDrugFormIconName(item.name)}
                    size={28}
                    color={colors.primary}
                  />
                </View>
              )}
              <View style={styles.cardInfo}>
                <Text style={styles.itemName} numberOfLines={1}>
                  {item.name}
                </Text>
                {(item.drugType || getPermitStatusLabel(item.cancelName)) && (
                  <View style={styles.typeRow}>
                    {item.drugType && (
                      <Text style={styles.drugType} numberOfLines={1}>
                        {item.drugType}
                      </Text>
                    )}
                    {getPermitStatusLabel(item.cancelName) && (
                      <View style={styles.statusBadge}>
                        <Text style={styles.statusBadgeText}>{getPermitStatusLabel(item.cancelName)}</Text>
                      </View>
                    )}
                  </View>
                )}
                {item.efficacy && (
                  <Text style={styles.efcyText} numberOfLines={2}>
                    {stripHtmlTags(item.efficacy)}
                  </Text>
                )}
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  searchContainer: {
    flexDirection: 'row',
    padding: spacing.lg,
    alignItems: 'center',
    gap: spacing.sm,
  },
  searchRow: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    height: 46,
  },
  searchIcon: { marginRight: spacing.sm },
  searchInput: { flex: 1, fontSize: font.body, color: colors.text },
  searchBtn: {
    backgroundColor: colors.primary,
    borderRadius: radius.md,
    height: 46,
    paddingHorizontal: spacing.lg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchBtnText: { color: colors.white, fontWeight: 'bold', fontSize: font.body },
  listContent: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xl },
  footerLoading: { marginVertical: spacing.lg },
  centerContainer: { alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  loadingText: { marginTop: spacing.md, fontSize: font.sub, color: colors.textSub },
  emptyText: { fontSize: font.body, color: colors.textSub },
  guideText: { marginTop: spacing.md, fontSize: font.body, color: colors.placeholder },
  card: {
    flexDirection: 'row',
    padding: spacing.md + 2,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    marginBottom: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
  },
  drugImage: { width: 70, height: 70, borderRadius: radius.sm, marginRight: spacing.md },
  noImage: {
    width: 70,
    height: 70,
    borderRadius: radius.sm,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  cardInfo: { flex: 1, justifyContent: 'center' },
  itemName: { fontSize: font.body + 1, fontWeight: 'bold', color: colors.text, marginBottom: 2 },
  typeRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginBottom: spacing.xs },
  drugType: { fontSize: font.sub, color: colors.primaryDark },
  statusBadge: {
    backgroundColor: colors.border,
    paddingHorizontal: spacing.sm,
    paddingVertical: 1,
    borderRadius: radius.sm,
  },
  statusBadgeText: { fontSize: font.caption, fontWeight: 'bold', color: colors.textSub },
  efcyText: { fontSize: font.sub, color: colors.textSub, lineHeight: 18 },
});

export default DrugSearchScreen;