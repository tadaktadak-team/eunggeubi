import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Feather, MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import DrugCard from '../components/DrugCard';

const DrugHomeScreen = ({ navigation }: any) => {
  const insets = useSafeAreaInsets();
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
    <View style={styles.container}>
      <View style={[styles.header, { paddingTop: insets.top + spacing.md }]}>
        <Text style={styles.headerTitle}>약물정보</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 검색창 */}
        <View style={styles.searchContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="약 이름으로 검색"
            value={searchQuery}
            onChangeText={setSearchQuery}
            onSubmitEditing={handleGoToSearch}
            returnKeyType="search"
            placeholderTextColor={colors.placeholder}
          />
          <TouchableOpacity onPress={handleGoToSearch} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
            <Feather name="search" size={18} color={colors.placeholder} />
          </TouchableOpacity>
        </View>

        {/* 중앙 기능 버튼 (낱알 특징 / 상호작용) */}
        <View style={styles.actionGrid}>
          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('PillSearch')}
            activeOpacity={0.7}
          >
            <View style={styles.actionIconBg}>
              <MaterialCommunityIcons name="pill" size={26} color={colors.primary} />
            </View>
            <Text style={styles.actionTitle}>낱알 특징</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.actionCard}
            onPress={() => navigation.navigate('InteractionCheck')}
            activeOpacity={0.7}
          >
            <View style={styles.actionIconBg}>
              <Ionicons name="pulse-outline" size={26} color={colors.primary} />
            </View>
            <Text style={styles.actionTitle}>상호작용</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.divider} />

        {/* 최근 검색 목록 */}
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
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  header: {
    paddingHorizontal: spacing.xl,
    paddingBottom: spacing.md,
  },
  headerTitle: {
    fontSize: font.h1,
    fontWeight: '800',
    color: colors.text,
  },
  scrollContent: {
    paddingHorizontal: spacing.xl,
    paddingBottom: spacing.xl,
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.lg,
    height: 48,
    marginBottom: spacing.lg,
  },
  searchInput: {
    flex: 1,
    fontSize: font.body,
    color: colors.text,
  },
  actionGrid: {
    flexDirection: 'row',
    gap: spacing.md,
    marginBottom: spacing.xl,
  },
  actionCard: {
    flex: 1,
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    paddingVertical: spacing.lg + 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  actionIconBg: {
    width: 48,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  actionTitle: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
  },
  divider: {
    height: 1,
    backgroundColor: colors.border,
    marginBottom: spacing.lg,
  },
  recentSection: {
    marginTop: 0,
  },
  sectionTitle: {
    fontSize: font.sub,
    fontWeight: '700',
    color: colors.textSub,
    marginBottom: spacing.md,
  },
});

export default DrugHomeScreen;