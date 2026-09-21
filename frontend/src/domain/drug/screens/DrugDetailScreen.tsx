import React from 'react';
import { View, Text, StyleSheet, ScrollView } from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

const DrugDetailScreen = ({ route }: any) => {
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
    <View style={styles.container}>
      <AppHeader title="약물 상세 정보" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 약물 요약 카드 */}
        <View style={styles.summaryCard}>
          <View style={styles.iconContainer}>
            <MaterialCommunityIcons name="pill" size={32} color={colors.primary} />
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
            <Feather name="alert-triangle" size={18} color={colors.danger} style={{ marginRight: spacing.xs }} />
            <Text style={styles.cautionTitle}>주의사항</Text>
          </View>
          <Text style={styles.cautionText}>{drugData.caution}</Text>
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
  scrollContent: {
    padding: spacing.xl,
    paddingBottom: spacing.xxl + spacing.md,
  },
  summaryCard: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg + 4,
    padding: spacing.xl,
    alignItems: 'center',
    marginBottom: spacing.xl,
  },
  iconContainer: {
    width: 64,
    height: 64,
    borderRadius: radius.lg,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  companyText: {
    fontSize: font.sub,
    color: colors.textSub,
    marginBottom: spacing.xs,
  },
  drugName: {
    fontSize: font.h2,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: spacing.sm + 2,
    textAlign: 'center',
  },
  badge: {
    backgroundColor: colors.primaryLight,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs + 2,
    borderRadius: radius.md,
  },
  badgeText: {
    fontSize: font.caption,
    fontWeight: 'bold',
    color: colors.primaryDark,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    fontSize: font.h3,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: spacing.sm + 2,
  },
  infoRow: {
    flexDirection: 'row',
    marginBottom: spacing.sm,
  },
  infoLabel: {
    width: 70,
    fontSize: font.body,
    color: colors.textSub,
  },
  infoValue: {
    flex: 1,
    fontSize: font.body,
    color: colors.text,
    fontWeight: '500',
  },
  bodyText: {
    fontSize: font.body,
    lineHeight: 22,
    color: colors.text,
    backgroundColor: colors.inputBg,
    padding: spacing.md + 2,
    borderRadius: radius.md,
  },
  cautionSection: {
    backgroundColor: colors.primaryLight,
    padding: spacing.lg,
    borderRadius: radius.lg - 2,
    borderWidth: 1,
    borderColor: colors.primaryLight,
  },
  cautionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.sm,
  },
  cautionTitle: {
    fontSize: font.body + 1,
    fontWeight: 'bold',
    color: colors.danger,
  },
  cautionText: {
    fontSize: font.sub,
    lineHeight: 20,
    color: colors.text,
  },
});

export default DrugDetailScreen;
