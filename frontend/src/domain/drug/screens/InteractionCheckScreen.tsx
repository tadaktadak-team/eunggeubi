import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

const InteractionCheckScreen = () => {
  const [inputText, setInputText] = useState('');
  const [selectedDrugs, setSelectedDrugs] = useState<string[]>([
    '타이레놀정 500mg',
    '아스피린정 100mg',
  ]);
  const [hasChecked, setHasChecked] = useState(false);

  // 약물 추가 함수
  const handleAddDrug = () => {
    if (inputText.trim()) {
      setSelectedDrugs([...selectedDrugs, inputText.trim()]);
      setInputText('');
      setHasChecked(false); // 새로 추가되면 결과 초기화
    }
  };

  // 약물 삭제 함수
  const handleRemoveDrug = (index: number) => {
    const updated = selectedDrugs.filter((_, i) => i !== index);
    setSelectedDrugs(updated);
    setHasChecked(false);
  };

  // 상호작용 검사 실행
  const handleCheck = () => {
    if (selectedDrugs.length < 2) {
      alert('비교할 약물을 2개 이상 추가해 주세요.');
      return;
    }
    setHasChecked(true);
  };

  return (
    <View style={styles.container}>
      <AppHeader title="상호작용 체크" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 설명 안내 */}
        <Text style={styles.subTitle}>
          함께 복용할 약물을 추가하고 병용 시 주의사항을 확인하세요.
        </Text>

        {/* 약물 검색 및 추가 입력창 */}
        <View style={styles.inputContainer}>
          <TextInput
            style={styles.input}
            placeholder="약물 이름을 입력하여 추가"
            value={inputText}
            onChangeText={setInputText}
            onSubmitEditing={handleAddDrug}
            placeholderTextColor={colors.placeholder}
          />
          <TouchableOpacity style={styles.addButton} onPress={handleAddDrug}>
            <Feather name="plus" size={20} color={colors.white} />
          </TouchableOpacity>
        </View>

        {/* 선택된 약물 태그 리스트 */}
        <View style={styles.drugListSection}>
          <Text style={styles.sectionLabel}>선택된 약물 ({selectedDrugs.length})</Text>
          <View style={styles.chipWrapper}>
            {selectedDrugs.map((drug, index) => (
              <View key={index} style={styles.drugChip}>
                <MaterialCommunityIcons
                  name="pill"
                  size={16}
                  color={colors.primary}
                  style={{ marginRight: spacing.xs }}
                />
                <Text style={styles.chipText}>{drug}</Text>
                <TouchableOpacity onPress={() => handleRemoveDrug(index)} style={styles.removeIcon}>
                  <Feather name="x" size={16} color={colors.placeholder} />
                </TouchableOpacity>
              </View>
            ))}
          </View>
        </View>

        {/* 상호작용 검사 결과 출력 영역 */}
        {hasChecked && (
          <View style={styles.resultSection}>
            <View style={styles.resultHeader}>
              <Ionicons name="warning-outline" size={22} color={colors.danger} />
              <Text style={styles.resultTitle}>병용 주의 필요</Text>
            </View>
            <Text style={styles.resultDesc}>
              <Text style={styles.boldText}>&lsquo;타이레놀정&rsquo;</Text>과{' '}
              <Text style={styles.boldText}>&lsquo;아스피린정&rsquo;</Text>을 함께 복용 시 위장관 출혈 위험이 증가할 수
              있습니다. 전문가(의사/약사)와 상의 후 복용을 권장합니다.
            </Text>
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
  inputContainer: {
    flexDirection: 'row',
    marginBottom: spacing.xl,
  },
  input: {
    flex: 1,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 48,
    fontSize: font.body,
    color: colors.text,
    marginRight: spacing.sm,
  },
  addButton: {
    width: 48,
    height: 48,
    backgroundColor: colors.black,
    borderRadius: radius.md,
    justifyContent: 'center',
    alignItems: 'center',
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
  },
  chipText: {
    fontSize: font.sub,
    color: colors.text,
    fontWeight: '500',
  },
  removeIcon: {
    marginLeft: spacing.sm - 2,
    padding: 2,
  },
  resultSection: {
    backgroundColor: colors.primaryLight,
    padding: spacing.lg,
    borderRadius: radius.lg,
    borderWidth: 1,
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
  resultDesc: {
    fontSize: font.body,
    lineHeight: 22,
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
