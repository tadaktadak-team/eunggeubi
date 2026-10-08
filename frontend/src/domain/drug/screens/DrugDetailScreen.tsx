import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, TouchableOpacity, Linking, Alert } from 'react-native';
import { Feather } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getDrugDetail, DrugInfoResponse } from '../api/drug';
import { stripHtmlTags } from '../../../shared/utils/html';
import { getPermitStatusLabel, isPrescriptionDrug } from '../utils/drugStatus';
import DrugImage from '../components/DrugImage';

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
  const permitStatusLabel = getPermitStatusLabel(drug.cancelName);
  const isPrescription = isPrescriptionDrug(drug.drugType);
  // 낱알식별에만 있고 e약은요·허가정보에는 없는 일반의약품은 본문(효능/용법/주의)이 하나도 없다.
  // 이때 외형 정보와 링크만 덩그러니 보이면 정보가 빠진 건지 알 수 없어서 안내 문구를 보여준다.
  const hasNoGuideText = !isPrescription && !drug.efficacy && !drug.useInfo && !drug.caution;

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
          {/* 식약처 약 사진은 전부 같은 가로세로 비율이라, 그 비율로 꽉 채워 흰 여백 없이 보여준다.
              사진이 없거나 못 불러오면 제형 아이콘 박스로 대체한다(DrugImage). */}
          <DrugImage variant="hero" uri={drug.itemImage} name={drug.name} />
          <Text style={styles.drugName}>{drug.name}</Text>
          <View style={styles.badgeRow}>
            {drug.drugType && (
              <View style={styles.badge}>
                <Text style={styles.badgeText}>{drug.drugType}</Text>
              </View>
            )}
            {permitStatusLabel && (
              <View style={styles.statusBadge}>
                <Text style={styles.statusBadgeText}>{permitStatusLabel}</Text>
              </View>
            )}
          </View>
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

        {hasNoGuideText && (
          <View style={styles.prescriptionNotice}>
            <Feather name="info" size={16} color={colors.textSub} style={{ marginRight: spacing.xs }} />
            <Text style={styles.prescriptionNoticeText}>
              이 약은 식약처 허가정보에 효능·용법·주의사항이 등록되어 있지 않아요. 복용 전 의사·약사와
              상담하거나 식약처 원문을 확인해 주세요.
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
  drugName: {
    fontSize: font.h2,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: spacing.sm + 2,
    textAlign: 'center',
  },
  badgeRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    gap: spacing.sm,
  },
  statusBadge: {
    backgroundColor: colors.border,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs + 2,
    borderRadius: radius.md,
  },
  statusBadgeText: {
    fontSize: font.caption,
    fontWeight: 'bold',
    color: colors.textSub,
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
