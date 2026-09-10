import React from 'react';
import {
  SafeAreaView,
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';

const DrugDetailScreen = ({ route, navigation }: any) => {
  // 이전 화면에서 전달받은 약물 ID (추후 API 연동 시 사용)
  const { drugId } = route.params || {};

  // 더미 상세 데이터 (추후 백엔드 API 데이터로 대체)
  const drugData = {
    name: '타이레놀정 500mg',
    company: '한국얀센',
    category: '해열·진통제',
    ingredient: '아세트아미노펜 500mg',
    appearance: '하얀색의 장방형 첩제',
    effect: '감기로 인한 발열 및 통증, 두통, 신경통, 근육통, 월경통, 치통 완화',
    usage: '성인 및 12세 이상 어린이: 1회 1~2정씩 1일 3~4회 필요시 복용 (4시간 이상 간격)',
    caution: '하루 최대 4,000mg을 초과하여 복용하지 마시오. 매일 세 잔 이상 정기적으로 술을 마시는 사람이 이 약을 복용해야 할 경우 의사 또는 약사와 상의해야 합니다.',
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* 상단 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Feather name="arrow-left" size={24} color="#111111" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>약물 상세 정보</Text>
        <View style={{ width: 24 }} /> {/* 중앙 정렬용 빈 공간 */}
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 약물 요약 카드 */}
        <View style={styles.summaryCard}>
          <View style={styles.iconContainer}>
            <MaterialCommunityIcons name="pill" size={32} color="#E53935" />
          </View>
          <Text style={styles.companyText}>{drugData.company}</Text>
          <Text style={styles.drugName}>{drugData.name}</Text>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{drugData.category}</Text>
          </View>
        </View>

        {/* 상세 정보 섹션들 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>주성분 및 제형</Text>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>성분명</Text>
            <Text style={styles.infoValue}>{drugData.ingredient}</Text>
          </View>
          <View style={styles.infoRow}>
            <Text style={styles.infoLabel}>외형</Text>
            <Text style={styles.infoValue}>{drugData.appearance}</Text>
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>효능 · 효과</Text>
          <Text style={styles.bodyText}>{drugData.effect}</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>용법 · 용량</Text>
          <Text style={styles.bodyText}>{drugData.usage}</Text>
        </View>

        <View style={[styles.section, styles.cautionSection]}>
          <View style={styles.cautionHeader}>
            <Feather name="alert-triangle" size={18} color="#D32F2F" style={{ marginRight: 6 }} />
            <Text style={styles.cautionTitle}>주의사항</Text>
          </View>
          <Text style={styles.cautionText}>{drugData.caution}</Text>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#F0F0F0',
  },
  backButton: {
    padding: 4,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#111111',
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 40,
  },
  summaryCard: {
    backgroundColor: '#F9F9F9',
    borderRadius: 20,
    padding: 24,
    alignItems: 'center',
    marginBottom: 24,
  },
  iconContainer: {
    width: 64,
    height: 64,
    borderRadius: 20,
    backgroundColor: '#FFF0F0',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 12,
  },
  companyText: {
    fontSize: 13,
    color: '#8E8E93',
    marginBottom: 4,
  },
  drugName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#111111',
    marginBottom: 10,
    textAlign: 'center',
  },
  badge: {
    backgroundColor: '#EBF3FF',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#0066FF',
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#111111',
    marginBottom: 10,
  },
  infoRow: {
    flexDirection: 'row',
    marginBottom: 8,
  },
  infoLabel: {
    width: 70,
    fontSize: 14,
    color: '#8E8E93',
  },
  infoValue: {
    flex: 1,
    fontSize: 14,
    color: '#333333',
    fontWeight: '500',
  },
  bodyText: {
    fontSize: 14,
    lineHeight: 22,
    color: '#444444',
    backgroundColor: '#FAFAFA',
    padding: 14,
    borderRadius: 12,
  },
  cautionSection: {
    backgroundColor: '#FFF8F8',
    padding: 16,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#FFEBEE',
  },
  cautionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  cautionTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#D32F2F',
  },
  cautionText: {
    fontSize: 13,
    lineHeight: 20,
    color: '#555555',
  },
});

export default DrugDetailScreen;