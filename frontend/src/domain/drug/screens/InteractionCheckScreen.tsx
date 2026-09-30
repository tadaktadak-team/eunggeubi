import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator, Alert } from 'react-native';
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { DrugInfoResponse } from '../api/drug';
import { checkInteractions, InteractionResponse } from '../api/interaction';
import { getDrugFormIconName } from '../utils/drugIcon';

type SelectedDrug = { itemSeq: string; name: string };

const InteractionCheckScreen = ({ navigation }: any) => {
  const [selectedDrugs, setSelectedDrugs] = useState<SelectedDrug[]>([]);
  const [results, setResults] = useState<InteractionResponse[]>([]);
  const [checking, setChecking] = useState(false);
  const [hasChecked, setHasChecked] = useState(false);

  // 검색화면을 "선택 모드"로 열어서 실제 약을 고르게 한다.
  const handleAddDrug = () => {
    navigation.navigate('DrugSearch', {
      selectMode: true,
      // e약은요(OTC 위주)는 병용금기 대상과 거의 안 겹쳐서, 우리 DB(전문의약품 포함) 검색을 쓴다.
      broadSearch: true,
      onSelect: (drug: DrugInfoResponse) => {
        setSelectedDrugs((prev) => {
          if (prev.some((d) => d.itemSeq === drug.itemSeq)) {
            // 이미 골라둔 약이면 그대로 둔다 (자기 자신끼리 비교되는 걸 방지)
            return prev;
          }
          return [...prev, { itemSeq: drug.itemSeq, name: drug.name }];
        });
        setHasChecked(false);
        setResults([]);
      },
    });
  };

  const handleRemoveDrug = (itemSeq: string) => {
    setSelectedDrugs((prev) => prev.filter((d) => d.itemSeq !== itemSeq));
    setHasChecked(false);
    setResults([]);
  };

  const handleCheck = async () => {
    if (selectedDrugs.length < 2) {
      Alert.alert('약물 추가 필요', '비교할 약물을 2개 이상 추가해 주세요.');
      return;
    }
    try {
      setChecking(true);
      const data = await checkInteractions(selectedDrugs.map((d) => d.itemSeq));
      setResults(data);
      setHasChecked(true);
    } catch (error) {
      console.error('상호작용 체크 오류:', error);
      Alert.alert('확인 실패', '상호작용 확인 중 오류가 발생했습니다. 서버 연결 상태를 확인해 주세요.');
    } finally {
      setChecking(false);
    }
  };

  return (
    <View style={styles.container}>
      <AppHeader title="상호작용 체크" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 설명 안내 */}
        <Text style={styles.subTitle}>
          함께 복용할 약물을 추가하고 병용 시 주의사항을 확인하세요.
        </Text>

        {/* 약물 추가 버튼 */}
        <TouchableOpacity style={styles.addRow} onPress={handleAddDrug} activeOpacity={0.7}>
          <Feather name="plus" size={18} color={colors.primaryDark} style={{ marginRight: spacing.xs }} />
          <Text style={styles.addRowText}>약물 검색해서 추가</Text>
        </TouchableOpacity>

        {/* 선택된 약물 태그 리스트 */}
        <View style={styles.drugListSection}>
          <Text style={styles.sectionLabel}>선택된 약물 ({selectedDrugs.length})</Text>
          {selectedDrugs.length === 0 ? (
            <Text style={styles.emptyText}>위 버튼으로 비교할 약을 추가해보세요.</Text>
          ) : (
            <View style={styles.chipWrapper}>
              {selectedDrugs.map((drug) => (
                <View key={drug.itemSeq} style={styles.drugChip}>
                  <MaterialCommunityIcons
                    name={getDrugFormIconName(drug.name)}
                    size={16}
                    color={colors.primary}
                    style={{ marginRight: spacing.xs }}
                  />
                  <Text style={styles.chipText} numberOfLines={1}>
                    {drug.name}
                  </Text>
                  <TouchableOpacity
                    onPress={() => handleRemoveDrug(drug.itemSeq)}
                    style={styles.removeIcon}
                  >
                    <Feather name="x" size={16} color={colors.placeholder} />
                  </TouchableOpacity>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 상호작용 검사 결과 출력 영역 */}
        {checking && (
          <View style={styles.centerContainer}>
            <ActivityIndicator size="large" color={colors.primary} />
          </View>
        )}

        {!checking && hasChecked && (
          <View
            style={[
              styles.resultSection,
              results.length === 0 ? styles.resultSectionSafe : styles.resultSectionWarning,
            ]}
          >
            {results.length === 0 ? (
              <View style={styles.resultHeader}>
                <Ionicons name="checkmark-circle-outline" size={22} color={colors.success} />
                <Text style={[styles.resultTitle, { color: colors.success }]}>병용 가능</Text>
              </View>
            ) : (
              <>
                <View style={styles.resultHeader}>
                  <Ionicons name="warning-outline" size={22} color={colors.danger} />
                  <Text style={styles.resultTitle}>병용 주의 필요 ({results.length}건)</Text>
                </View>
                {results.map((r, index) => (
                  <View key={`${r.itemSeqA}_${r.itemSeqB}`} style={index > 0 ? styles.resultItem : undefined}>
                    <Text style={styles.resultDesc}>
                      <Text style={styles.boldText}>&lsquo;{r.itemNameA}&rsquo;</Text>과{' '}
                      <Text style={styles.boldText}>&lsquo;{r.itemNameB}&rsquo;</Text>: {r.reason}
                    </Text>
                  </View>
                ))}
                <Text style={styles.resultFooter}>전문가(의사/약사)와 상의 후 복용을 권장합니다.</Text>
              </>
            )}
          </View>
        )}
      </ScrollView>

      {/* 하단 검사하기 버튼 */}
      <View style={styles.bottomContainer}>
        <TouchableOpacity style={styles.checkButton} onPress={handleCheck} activeOpacity={0.8}>
          <Ionicons name="pulse-outline" size={20} color={colors.white} style={{ marginRight: spacing.sm }} />
          <Text style={styles.checkButtonText}>상호작용 확인하기</Text>
        </TouchableOpacity>
      </View>
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
  },
  subTitle: {
    fontSize: font.body,
    color: colors.textSub,
    marginBottom: spacing.xl,
    lineHeight: 20,
  },
  addRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.primaryLight,
    borderRadius: radius.md,
    height: 48,
    marginBottom: spacing.xl,
  },
  addRowText: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.primaryDark,
  },
  drugListSection: {
    marginBottom: spacing.xl,
  },
  sectionLabel: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.textSub,
    marginBottom: spacing.md,
  },
  emptyText: {
    fontSize: font.sub,
    color: colors.placeholder,
  },
  chipWrapper: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  drugChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    maxWidth: '100%',
  },
  chipText: {
    fontSize: font.sub,
    color: colors.text,
    fontWeight: '500',
    maxWidth: 180,
  },
  removeIcon: {
    marginLeft: spacing.sm - 2,
    padding: 2,
  },
  centerContainer: {
    alignItems: 'center',
    paddingVertical: spacing.xl,
  },
  resultSection: {
    padding: spacing.lg,
    borderRadius: radius.lg,
    borderWidth: 1,
  },
  resultSectionSafe: {
    // 테마에 연한 초록이 따로 없어서 success(#22C55E) 기반 연한 배경을 직접 지정
    backgroundColor: '#E9F9EF',
    borderColor: '#E9F9EF',
  },
  resultSectionWarning: {
    backgroundColor: colors.primaryLight,
    borderColor: colors.primaryLight,
  },
  resultHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: spacing.sm + 2,
    gap: spacing.xs + 2,
  },
  resultTitle: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.danger,
  },
  resultItem: {
    marginTop: spacing.sm,
    paddingTop: spacing.sm,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  resultDesc: {
    fontSize: font.body,
    lineHeight: 22,
    color: colors.text,
  },
  resultFooter: {
    marginTop: spacing.md,
    fontSize: font.sub,
    color: colors.textSub,
  },
  boldText: {
    fontWeight: 'bold',
    color: colors.text,
  },
  bottomContainer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.white,
  },
  checkButton: {
    backgroundColor: colors.black,
    borderRadius: radius.md,
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkButtonText: {
    color: colors.white,
    fontSize: font.body,
    fontWeight: 'bold',
  },
});

export default InteractionCheckScreen;
