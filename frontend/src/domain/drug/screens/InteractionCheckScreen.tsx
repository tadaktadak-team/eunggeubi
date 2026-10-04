import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator, Alert } from 'react-native';
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { checkInteractions, InteractionResponse } from '../api/interaction';
import { getDrugFormIconName } from '../utils/drugIcon';

type SelectedDrug = { itemSeq: string; name: string };

const InteractionCheckScreen = ({ navigation, route }: any) => {
  const [selectedDrugs, setSelectedDrugs] = useState<SelectedDrug[]>([]);
  const [results, setResults] = useState<InteractionResponse[]>([]);
  const [checking, setChecking] = useState(false);
  const [hasChecked, setHasChecked] = useState(false);

  // 검색화면을 "선택 모드"로 열어서 실제 약을 고르게 한다. 고른 약은 콜백 함수가 아니라
  // 검색화면이 popTo로 돌아오면서 넘겨주는 route.params.selectedDrug(값)로 받는다.
  // (함수를 화면 파라미터로 넘기면 네비게이션 상태가 직렬화되지 않아 경고가 나고, 상태 복원 등에서
  // 문제가 생길 수 있다.)
  const handleAddDrug = () => {
    navigation.navigate('DrugSearch', { selectMode: true });
  };

  const selectedFromSearch: SelectedDrug | undefined = route?.params?.selectedDrug;
  useEffect(() => {
    if (!selectedFromSearch) return;
    // 받은 약을 목록에 추가하고, 같은 값을 다시 처리하지 않도록 파라미터를 비운다.
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 화면 파라미터로 전달된 값을 1회 반영
    setSelectedDrugs((prev) =>
      // 이미 골라둔 약이면 그대로 둔다 (자기 자신끼리 비교되는 걸 방지)
      prev.some((d) => d.itemSeq === selectedFromSearch.itemSeq) ? prev : [...prev, selectedFromSearch],
    );
    setHasChecked(false);
    setResults([]);
    navigation.setParams({ selectedDrug: undefined });
  }, [selectedFromSearch, navigation]);

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
              <>
                <View style={styles.resultHeader}>
                  <Ionicons name="information-circle-outline" size={22} color={colors.textSub} />
                  <Text style={[styles.resultTitle, { color: colors.text }]}>
                    병용금기 목록에는 해당하지 않아요
                  </Text>
                </View>
                <Text style={styles.resultFooter}>
                  다른 상호작용은 의사·약사와 상의하세요.
                </Text>
              </>
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
                      <Text style={styles.boldText}>&lsquo;{r.itemNameB}&rsquo;</Text>
                    </Text>
                    {r.reasons.map((reason) => (
                      <Text key={reason} style={styles.reasonText}>
                        • {reason}
                      </Text>
                    ))}
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
  // "금기 목록에 없음"은 안전하다는 보증이 아니라서 초록(안전) 대신 중립 색을 쓴다.
  resultSectionSafe: {
    backgroundColor: colors.inputBg,
    borderColor: colors.border,
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
  reasonText: {
    marginTop: spacing.xs,
    fontSize: font.sub,
    lineHeight: 20,
    color: colors.text,
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
