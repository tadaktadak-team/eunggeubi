import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useRef, useState } from 'react';
import { ActivityIndicator, Alert, FlatList, Image, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { searchMedications } from '../api/health';
import { MedicationItem, MedicationSearchItem, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

export default function MedicationPickerScreen() {
  const navigation = useNavigation<Nav>();
  const [keyword, setKeyword] = useState('');
  const [searchedKeyword, setSearchedKeyword] = useState('');
  const [results, setResults] = useState<MedicationSearchItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const requestIdRef = useRef(0);

  const search = async () => {
    const kw = keyword.trim();
    if (!kw) return;
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setSearched(true);
    setSearchedKeyword(kw);
    try {
      const list = await searchMedications(kw);
      if (requestId !== requestIdRef.current) return;
      setResults(list);
    } catch (e: any) {
      if (requestId !== requestIdRef.current) return;
      setResults([]);
      Alert.alert('검색 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  };

  const pick = (item: MedicationItem) =>
    navigation.popTo('Health', { pickedMedication: item }, { merge: true });

  return (
    <View style={styles.container}>
      <AppHeader title="복용약 검색" />

      <View style={styles.searchRow}>
        <View style={styles.inputWrap}>
          <Feather name="search" size={18} color={colors.placeholder} />
          <TextInput
            style={styles.input}
            value={keyword}
            onChangeText={setKeyword}
            placeholder="약 이름을 입력하세요"
            placeholderTextColor={colors.placeholder}
            onSubmitEditing={search}
            returnKeyType="search"
            autoFocus
          />
          {keyword.length > 0 && (
            <Pressable onPress={() => setKeyword('')} hitSlop={8}>
              <Ionicons name="close-circle" size={18} color={colors.placeholder} />
            </Pressable>
          )}
        </View>
        <Pressable style={styles.searchBtn} onPress={search}>
          <Text style={styles.searchBtnText}>검색</Text>
        </Pressable>
      </View>

      {loading ? (
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item) => item.itemSeq}
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <Text style={styles.guide}>
              {searched ? '검색 결과가 없어요.' : '복용 중인 약 이름을 검색해서 골라주세요.\n처방약도 검색돼요.'}
            </Text>
          }
          renderItem={({ item }) => (
            <Pressable style={styles.row} onPress={() => pick({ name: item.name, itemSeq: item.itemSeq })}>
              {item.itemImage ? (
                <Image source={{ uri: item.itemImage }} style={styles.image} />
              ) : (
                <View style={styles.noImage}>
                  <MaterialCommunityIcons name="pill" size={24} color={colors.primary} />
                </View>
              )}
              <View style={styles.info}>
                <Text style={styles.name} numberOfLines={2}>
                  {item.name}
                </Text>
                {item.drugType && (
                  <Text style={[styles.type, item.drugType === '전문의약품' && styles.typePro]}>{item.drugType}</Text>
                )}
              </View>
              <Feather name="plus-circle" size={20} color={colors.primary} />
            </Pressable>
          )}
          ListFooterComponent={
            searched ? (
              <Pressable style={styles.manualBtn} onPress={() => pick({ name: searchedKeyword, itemSeq: null })}>
                <Text style={styles.manualText}>찾는 약이 없나요? ‘{searchedKeyword}’ 그대로 직접 추가</Text>
              </Pressable>
            ) : null
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  searchRow: { flexDirection: 'row', gap: spacing.sm, padding: spacing.lg },
  inputWrap: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    height: 46,
  },
  input: { flex: 1, fontSize: font.body, color: colors.text },
  searchBtn: {
    backgroundColor: colors.primary,
    borderRadius: radius.md,
    height: 46,
    paddingHorizontal: spacing.lg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  searchBtnText: { color: colors.white, fontWeight: '700', fontSize: font.body },
  listContent: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xxl },
  guide: { textAlign: 'center', color: colors.placeholder, fontSize: font.body, lineHeight: 22, marginTop: spacing.xxl },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  image: { width: 48, height: 48, borderRadius: radius.sm },
  noImage: {
    width: 48,
    height: 48,
    borderRadius: radius.sm,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: { flex: 1, gap: 2 },
  name: { fontSize: font.body, fontWeight: '700', color: colors.text },
  type: { fontSize: font.caption, color: colors.textSub, fontWeight: '600' },
  typePro: { color: colors.danger },
  manualBtn: { alignItems: 'center', paddingVertical: spacing.lg },
  manualText: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
});
