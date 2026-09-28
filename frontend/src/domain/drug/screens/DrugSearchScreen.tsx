import React, { useState, useEffect } from 'react';
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

const NUM_OF_ROWS = 10;

const DrugSearchScreen: React.FC<{ navigation: any; route: any }> = ({ navigation, route }) => {
  const initialKeyword: string = route?.params?.initialKeyword ?? '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [searchedKeyword, setSearchedKeyword] = useState('');
  const [drugs, setDrugs] = useState<DrugInfoResponse[]>([]);
  const [pageNo, setPageNo] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [searched, setSearched] = useState(false);

  // 새 검색어로 첫 페이지부터 다시 검색
  const runSearch = async (kw: string) => {
    if (!kw.trim()) return;
    try {
      setLoading(true);
      setSearched(true);
      setSearchedKeyword(kw.trim());
      const result = await searchDrugsByName(kw.trim(), 1, NUM_OF_ROWS);
      setDrugs(result.items);
      setPageNo(1);
      setTotalCount(result.totalCount);
    } catch (error) {
      console.error('약품 검색 오류:', error);
      Alert.alert('검색 실패', '검색 중 오류가 발생했습니다. 서버 연결 상태를 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  // 스크롤이 끝에 닿으면 다음 페이지를 이어붙임
  const loadMore = async () => {
    if (loading || loadingMore) return;
    if (drugs.length >= totalCount) return;

    const nextPage = pageNo + 1;
    try {
      setLoadingMore(true);
      const result = await searchDrugsByName(searchedKeyword, nextPage, NUM_OF_ROWS);
      setDrugs((prev) => [...prev, ...result.items]);
      setPageNo(nextPage);
      setTotalCount(result.totalCount);
    } catch (error) {
      console.error('약품 검색 추가 로딩 오류:', error);
    } finally {
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
      <AppHeader title="약품명 검색" />

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
                    color={colors.placeholder}
                  />
                </View>
              )}
              <View style={styles.cardInfo}>
                <Text style={styles.itemName} numberOfLines={1}>
                  {item.name}
                </Text>
                {item.drugType && (
                  <Text style={styles.drugType} numberOfLines={1}>
                    {item.drugType}
                  </Text>
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
    backgroundColor: colors.border,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: spacing.md,
  },
  cardInfo: { flex: 1, justifyContent: 'center' },
  itemName: { fontSize: font.body + 1, fontWeight: 'bold', color: colors.text, marginBottom: 2 },
  drugType: { fontSize: font.sub, color: colors.primaryDark, marginBottom: spacing.xs },
  efcyText: { fontSize: font.sub, color: colors.textSub, lineHeight: 18 },
});

export default DrugSearchScreen;