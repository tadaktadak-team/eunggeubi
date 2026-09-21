import AsyncStorage from '@react-native-async-storage/async-storage';

const STORAGE_KEY = 'drug_recent_searches';
const MAX_ITEMS = 10;

export interface RecentDrug {
  itemSeq: string;
  name: string;
  drugType?: string;
}

export async function getRecentSearches(): Promise<RecentDrug[]> {
  try {
    const raw = await AsyncStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as RecentDrug[]) : [];
  } catch {
    return [];
  }
}

// 같은 약을 다시 검색하면 맨 앞으로 올리고(중복 제거), 최대 개수를 넘으면 오래된 것부터 버림
export async function addRecentSearch(drug: RecentDrug): Promise<void> {
  try {
    const current = await getRecentSearches();
    const deduped = current.filter((d) => d.itemSeq !== drug.itemSeq);
    const next = [drug, ...deduped].slice(0, MAX_ITEMS);
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // 최근 검색 저장은 부가 기능이라 실패해도 검색 흐름 자체엔 영향 주지 않음
  }
}
