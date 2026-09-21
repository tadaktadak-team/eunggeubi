import React, { useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { Feather, MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import DrugCard from '../components/DrugCard';

const DrugHomeScreen = ({ navigation }: any) => {
  const [searchQuery, setSearchQuery] = useState('');

  const recentDrugs = [
    { id: '1', name: '타이레놀정', dosage: '500mg', category: '해열·진통제' },
    { id: '2', name: '아스피린', dosage: '100mg', category: '항혈소판제' },
  ];

  // 검색창 -> 검색 결과 화면으로 이동 (입력된 검색어를 함께 전달)
  const handleGoToSearch = () => {
    navigation.navigate('DrugSearch', { initialKeyword: searchQuery });
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>

        {/* 1. 상단 타이틀 */}
        <Text style={styles.headerTitle}>약물정보</Text>

        {/* 2. 검색창 (우측 돋보기 아이콘) */}
        <View style={styles.searchContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="약 이름으로 검색"
            value={searchQuery}
            onChangeText={setSearchQuery}
            onSubmitEditing={handleGoToSearch}
            returnKeyType="search"
            placeholderTextColor="#A0A0A0"
          />
          <TouchableOpacity onPress={handleGoToSearch} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
            <Feather name="search" size={18} color="#A0A0A0" />
          </TouchableOpacity>
        </View>

        {/* 3. 중앙 기능 버튼 (낱알 특징 / 상호작용) */}
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('PillSearch')}
            activeOpacity={0.7}
          >
            <View style={styles.actionIconBg}>
              <MaterialCommunityIcons name="pill" size={26} color="#555555" />
            </View>
            <Text style={styles.actionTitle}>낱알 특징</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('InteractionCheck')}
            activeOpacity={0.7}
          >
            <View style={styles.actionIconBg}>
              <Ionicons name="pulse-outline" size={26} color="#555555" />
            </View>
            <Text style={styles.actionTitle}>상호작용</Text>
          </TouchableOpacity>
        </View>

        {/* 4. 구분선 */}
        <View style={styles.divider} />

        {/* 5. 최근 검색 목록 */}
        <View style={styles.recentSection}>
          <Text style={styles.sectionTitle}>최근 검색</Text>
          {recentDrugs.map((drug) => (
            <DrugCard
              key={drug.id}
              name={drug.name}
              dosage={drug.dosage}
              category={drug.category}
              onPress={() => navigation.navigate('DrugDetail', { drugId: drug.id })}
            />
          ))}
        </View>

      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  scrollContent: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 24,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#111111',
    marginBottom: 16,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    borderRadius: 20,
    paddingHorizontal: 16,
    height: 48,
    marginBottom: 16,
  },
  searchInput: {
    flex: 1,
    fontSize: 15,
    color: '#111111',
  },
  actionGrid: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 20,
  },
  actionCard: {
    flex: 1,
    backgroundColor: '#F5F5F5',
    borderRadius: 16,
    paddingVertical: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionIconBg: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: '#E8E8E8',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
  },
  actionTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#111111',
  },
  divider: {
    height: 1,
    backgroundColor: '#EEEEEE',
    marginBottom: 16,
  },
  recentSection: {
    marginTop: 0,
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#8E8E93',
    marginBottom: 12,
  },
});

export default DrugHomeScreen;