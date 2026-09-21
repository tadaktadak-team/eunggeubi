import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  StyleSheet,
  Image,
} from 'react-native';
import { Feather, Ionicons } from '@expo/vector-icons';
import { searchDrugsByName, DrugInfoResponse } from '../api/drug';

const DrugSearchScreen: React.FC<{ navigation: any; route: any }> = ({ navigation, route }) => {
  const initialKeyword: string = route?.params?.initialKeyword ?? '';

  const [keyword, setKeyword] = useState(initialKeyword);
  const [drugs, setDrugs] = useState<DrugInfoResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const runSearch = async (kw: string) => {
    if (!kw.trim()) return;
    try {
      setLoading(true);
      setSearched(true);
      const results = await searchDrugsByName(kw.trim());
      setDrugs(results);
    } catch (error) {
      console.error('약품 검색 오류:', error);
      alert('검색 중 오류가 발생했습니다. 서버 연결 상태를 확인해 주세요.');
    } finally {
      setLoading(false);
    }
  };

  // 홈 화면에서 넘어온 검색어가 있으면 진입 시 한 번만 자동 검색
  useEffect(() => {
    if (initialKeyword.trim()) {
      runSearch(initialKeyword);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => runSearch(keyword);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Feather name="arrow-left" size={22} color="#111111" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>약품명 검색</Text>
        <View style={{ width: 22 }} />
      </View>

      <View style={styles.searchContainer}>
        <View style={styles.searchRow}>
          <Feather name="search" size={18} color="#A0A0A0" style={styles.searchIcon} />
          <TextInput
            style={styles.searchInput}
            placeholder="약 이름을 입력하세요"
            placeholderTextColor="#A0A0A0"
            value={keyword}
            onChangeText={setKeyword}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
          />
          {keyword.length > 0 && (
            <TouchableOpacity onPress={() => setKeyword('')}>
              <Ionicons name="close-circle" size={18} color="#A0A0A0" />
            </TouchableOpacity>
          )}
        </View>
        <TouchableOpacity style={styles.searchBtn} onPress={handleSearch}>
          <Text style={styles.searchBtnText}>검색</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.centerContainer}>
          <ActivityIndicator size="large" color="#0066FF" />
          <Text style={styles.loadingText}>약 정보를 불러오는 중...</Text>
        </View>
      ) : (
        <FlatList
          data={drugs}
          keyExtractor={(item) => item.itemSeq}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            searched ? (
              <View style={styles.centerContainer}>
                <Text style={styles.emptyText}>검색 결과가 없습니다.</Text>
              </View>
            ) : (
              <View style={styles.centerContainer}>
                <Feather name="search" size={32} color="#D0D0D0" />
                <Text style={styles.guideText}>궁금한 약의 이름을 검색해 보세요.</Text>
              </View>
            )
          }
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.card}
              onPress={() => navigation.navigate('DrugDetail', { itemSeq: item.itemSeq })}
            >
              {item.itemImage ? (
                <Image source={{ uri: item.itemImage }} style={styles.drugImage} />
              ) : (
                <View style={styles.noImage}>
                  <Feather name="image" size={22} color="#C0C0C0" />
                </View>
              )}
              <View style={styles.cardInfo}>
                <Text style={styles.itemName} numberOfLines={1}>{item.itemName}</Text>
                {item.entpName && (
                  <Text style={styles.entpName} numberOfLines={1}>{item.entpName}</Text>
                )}
                {item.efcyQesitm && (
                  <Text style={styles.efcyText} numberOfLines={2}>
                    {item.efcyQesitm.replace(/<[^>]*>?/g, '')}
                  </Text>
                )}
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFFFFF' },
  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: '#F0F0F0',
  },
  backButton: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#111111' },
  searchContainer: { flexDirection: 'row', padding: 16, alignItems: 'center', gap: 8 },
  searchRow: {
    flex: 1, flexDirection: 'row', alignItems: 'center',
    backgroundColor: '#F5F5F5', borderRadius: 12, paddingHorizontal: 12, height: 46,
  },
  searchIcon: { marginRight: 8 },
  searchInput: { flex: 1, fontSize: 15, color: '#111111' },
  searchBtn: {
    backgroundColor: '#0066FF', borderRadius: 12, height: 46,
    paddingHorizontal: 16, justifyContent: 'center', alignItems: 'center',
  },
  searchBtnText: { color: '#FFFFFF', fontWeight: 'bold', fontSize: 15 },
  listContent: { paddingHorizontal: 16, paddingBottom: 20 },
  centerContainer: { alignItems: 'center', justifyContent: 'center', paddingTop: 80 },
  loadingText: { marginTop: 12, fontSize: 14, color: '#666' },
  emptyText: { fontSize: 15, color: '#888' },
  guideText: { marginTop: 12, fontSize: 15, color: '#A0A0A0' },
  card: {
    flexDirection: 'row', padding: 14, borderRadius: 12,
    backgroundColor: '#FAFAFA', marginBottom: 12, borderWidth: 1, borderColor: '#EEEEEE',
  },
  drugImage: { width: 70, height: 70, borderRadius: 8, marginRight: 12 },
  noImage: {
    width: 70, height: 70, borderRadius: 8, backgroundColor: '#EAEAEA',
    justifyContent: 'center', alignItems: 'center', marginRight: 12,
  },
  cardInfo: { flex: 1, justifyContent: 'center' },
  itemName: { fontSize: 16, fontWeight: 'bold', color: '#111111', marginBottom: 2 },
  entpName: { fontSize: 13, color: '#0066FF', marginBottom: 4 },
  efcyText: { fontSize: 13, color: '#666666', lineHeight: 18 },
});

export default DrugSearchScreen;