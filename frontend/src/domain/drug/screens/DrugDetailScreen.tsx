import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, Image, TouchableOpacity, Linking, Alert } from 'react-native';
import { Feather, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getDrugDetail, DrugInfoResponse } from '../api/drug';
import { stripHtmlTags } from '../../../shared/utils/html';
import { getDrugFormIconName } from '../utils/drugIcon';

// 우리 DB에 효능/용법/주의사항이 비어있거나 요약돼 있어도, 식약처 원문(의약품안전나라)은
// itemSeq(cacheSeq)만 있으면 모든 약에 대해 항상 조회 가능 — 공백을 메우는 안전망으로 제공.
function buildMfdsUrl(itemSeq: string): string {
  return `https://nedrug.mfds.go.kr/pbp/CCBBB01/getItemDetailCache?cacheSeq=${itemSeq}`;
}

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
  // 전문의약품의 효능/용법/주의사항은 식약처 허가정보 원문(임상시험 수치, 금기 목록 등 규제
  // 문서 그대로)이라 일반의약품(e약은요, 짧은 소비자용 문구)과 결이 너무 달라 화면에서는
  // 생략하고 외형정보 + 식약처 원문 링크만 보여준다. DB에는 그대로 보관돼 있음(향후 활용 대비).
  const isPrescription = drug.drugType === '전문의약품' || drug.drugType === '전문,희귀';

  const handleOpenMfds = () => {
    Linking.openURL(buildMfdsUrl(drug.itemSeq)).catch(() =>
      Alert.alert('오류', '식약처 페이지를 열 수 없어요.'),
    );
  };

  return (
    <View style={styles.container}>
      <AppHeader title="약물 상세 정보" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 약물 요약 카드 */}
        <View style={styles.summaryCard}>
          {/* 식약처 실측 사진은 자 눈금까지 찍힌 가로로 긴 사진이라, 아이콘 대체용 정사각형
              박스에 넣으면 여백이 어색하게 남는다. 사진이 있을 땐 비율 그대로 넓게 보여주고,
              없을 때만 정사각형 아이콘 박스를 쓴다. */}
          {drug.itemImage ? (
            <Image source={{ uri: drug.itemImage }} style={styles.drugPhoto} resizeMode="contain" />
          ) : (
            <View style={styles.iconContainer}>
              <MaterialCommunityIcons name={getDrugFormIconName(drug.name)} size={56} color={colors.primary} />
            </View>
          )}
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

        {!isPrescription && drug.efficacy && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>효능 · 효과</Text>
            <Text style={styles.bodyText}>{stripHtmlTags(drug.efficacy)}</Text>
          </View>
        )}

        {!isPrescription && drug.useInfo && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>용법 · 용량</Text>
            <Text style={styles.bodyText}>{stripHtmlTags(drug.useInfo)}</Text>
          </View>
        )}

        {!isPrescription && drug.caution && (
          <View style={[styles.section, styles.cautionSection]}>
            <View style={styles.cautionHeader}>
              <Feather name="alert-triangle" size={18} color={colors.danger} style={{ marginRight: spacing.xs }} />
              <Text style={styles.cautionTitle}>주의사항</Text>
            </View>
            <Text style={styles.cautionText}>{stripHtmlTags(drug.caution)}</Text>
          </View>
        )}

        {isPrescription && (
          <View style={styles.prescriptionNotice}>
            <Feather name="info" size={16} color={colors.textSub} style={{ marginRight: spacing.xs }} />
            <Text style={styles.prescriptionNoticeText}>
              전문의약품은 의사·약사의 복약지도에 따라 복용해야 합니다. 상세 정보는 식약처
              공식 문서를 통해 확인하시기 바랍니다.
            </Text>
          </View>
        )}

        <TouchableOpacity style={styles.mfdsLink} onPress={handleOpenMfds} activeOpacity={0.7}>
          <Feather name="external-link" size={16} color={colors.textSub} style={{ marginRight: spacing.xs }} />
          <Text style={styles.mfdsLinkText}>식약처에서 원문 보기</Text>
        </TouchableOpacity>
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
    width: 120,
    height: 120,
    borderRadius: radius.lg + 8,
    backgroundColor: colors.primaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: spacing.md,
    overflow: 'hidden',
  },
  drugPhoto: {
    width: '100%',
    height: 140,
    borderRadius: radius.lg,
    backgroundColor: colors.white,
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
  prescriptionNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    padding: spacing.md + 2,
    borderRadius: radius.md,
    marginBottom: spacing.sm,
  },
  prescriptionNoticeText: {
    flex: 1,
    fontSize: font.sub,
    lineHeight: 18,
    color: colors.textSub,
  },
  mfdsLink: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.md,
  },
  mfdsLinkText: {
    fontSize: font.sub,
    color: colors.textSub,
    textDecorationLine: 'underline',
  },
});

export default DrugDetailScreen;
