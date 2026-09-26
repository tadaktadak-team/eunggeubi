import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator } from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getDrugDetail, DrugInfoResponse } from '../api/drug';

// 값이 있는 항목만 화면에 표시 (없는 걸 빈 줄로 보여주면 오히려 오해를 줌)
function buildAppearanceRows(drug: DrugInfoResponse): { label: string; value: string }[] {
  const rows: { label: string; value: string }[] = [];
  if (drug.shape) rows.push({ label: '모양', value: drug.shape });
  if (drug.color) rows.push({ label: '색상', value: drug.color });
  if (drug.imprint) rows.push({ label: '식별문자', value: drug.imprint });
  return rows;
}

const DrugDetailScreen = ({ route }: any) => {
  const { itemSeq } = route.params || {};

  const [drug, setDrug] = useState<DrugInfoResponse | null>(null);
  const [loading, setLoading] = useState(!!itemSeq);
  const [error, setError] = useState(!itemSeq);

  useEffect(() => {
    if (!itemSeq) return; // itemSeq 없으면 초기 state(error=true)로 이미 처리됨

    (async () => {
      try {
        const data = await getDrugDetail(itemSeq);
        setDrug(data);
        setError(false);
      } catch (e) {
        console.error('약품 상세 조회 오류:', e);
        setError(true);
      } finally {
        setLoading(false);
      }
    })();
  }, [itemSeq]);

  if (loading) {
    return (
      <View style={styles.container}>
        <AppHeader title="약물 상세 정보" />
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      </View>
    );
  }

  if (error || !drug) {
    return (
      <View style={styles.container}>
        <AppHeader title="약물 상세 정보" />
        <View style={styles.centerContainer}>
          <Feather name="alert-circle" size={28} color={colors.placeholder} />
          <Text style={styles.errorText}>약물 정보를 불러오지 못했어요.</Text>
        </View>
      </View>
    );
  }

  const appearanceRows = buildAppearanceRows(drug);

  return (
    <View style={styles.container}>
      <AppHeader title="약물 상세 정보" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 약물 요약 카드 */}
        <View style={styles.summaryCard}>
          <View style={styles.iconContainer}>
            <MaterialCommunityIcons name="pill" size={32} color={colors.primary} />
          </View>
          <Text style={styles.drugName}>{drug.name}</Text>
          {drug.drugType && (
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{drug.drugType}</Text>
            </View>
          )}
        </View>

        {/* 외형 정보 */}
        {appearanceRows.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>외형 정보</Text>
            {appearanceRows.map((row) => (
              <View key={row.label} style={styles.infoRow}>
                <Text style={styles.infoLabel}>{row.label}</Text>
                <Text style={styles.infoValue}>{row.value}</Text>
              </View>
            ))}
          </View>
        )}

        {drug.efficacy && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>효능 · 효과</Text>
            <Text style={styles.bodyText}>{drug.efficacy}</Text>
          </View>
        )}

        {drug.useInfo && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>용법 · 용량</Text>
            <Text style={styles.bodyText}>{drug.useInfo}</Text>
          </View>
        )}

        {drug.caution && (
          <View style={[styles.section, styles.cautionSection]}>
            <View style={styles.cautionHeader}>
              <Feather name="alert-triangle" size={18} color={colors.danger} style={{ marginRight: spacing.xs }} />
              <Text style={styles.cautionTitle}>주의사항</Text>
            </View>
            <Text style={styles.cautionText}>{drug.caution}</Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  centerContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.xl,
  },
  errorText: {
    fontSize: font.body,
    color: colors.textSub,
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
