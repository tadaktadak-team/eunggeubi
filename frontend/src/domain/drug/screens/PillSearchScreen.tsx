import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

const PillSearchScreen = () => {
  const [printText, setPrintText] = useState('');
  const [selectedShape, setSelectedShape] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [selectedForm, setSelectedForm] = useState('');

  // 필터 옵션 데이터
  const shapes = ['원형', '타원형', '장방형', '삼각형', '사각형', '기타'];
  const colorOptions = ['하양', '노랑', '주황', '분홍', '빨강', '갈색', '연두', '초록', '파랑'];
  const forms = ['정제', '경질캡슐', '연질캡슐'];

  const handleReset = () => {
    setPrintText('');
    setSelectedShape('');
    setSelectedColor('');
    setSelectedForm('');
  };

  return (
    <View style={styles.container}>
      <AppHeader title="낱알 특징 검색" />

      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.resetRow}>
          <TouchableOpacity onPress={handleReset} hitSlop={8}>
            <Text style={styles.resetText}>초기화</Text>
          </TouchableOpacity>
        </View>

        {/* 식별문자 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>식별문자</Text>
          <TextInput
            style={styles.textInput}
            placeholder="알약에 적힌 글자 (예: TY, 500)"
            value={printText}
            onChangeText={setPrintText}
            placeholderTextColor={colors.placeholder}
          />
        </View>

        {/* 모양 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>모양</Text>
          <View style={styles.chipContainer}>
            {shapes.map((shape) => (
              <TouchableOpacity
                key={shape}
                style={[styles.chip, selectedShape === shape && styles.chipSelected]}
                onPress={() => setSelectedShape(selectedShape === shape ? '' : shape)}
              >
                <Text style={[styles.chipText, selectedShape === shape && styles.chipTextSelected]}>
                  {shape}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 색상 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>색상</Text>
          <View style={styles.chipContainer}>
            {colorOptions.map((color) => (
              <TouchableOpacity
                key={color}
                style={[styles.chip, selectedColor === color && styles.chipSelected]}
                onPress={() => setSelectedColor(selectedColor === color ? '' : color)}
              >
                <Text style={[styles.chipText, selectedColor === color && styles.chipTextSelected]}>
                  {color}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 제형 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>제형</Text>
          <View style={styles.chipContainer}>
            {forms.map((form) => (
              <TouchableOpacity
                key={form}
                style={[styles.chip, selectedForm === form && styles.chipSelected]}
                onPress={() => setSelectedForm(selectedForm === form ? '' : form)}
              >
                <Text style={[styles.chipText, selectedForm === form && styles.chipTextSelected]}>
                  {form}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </ScrollView>

      {/* 하단 검색하기 버튼 */}
      <View style={styles.bottomContainer}>
        <TouchableOpacity style={styles.searchButton} activeOpacity={0.8}>
          <Ionicons name="search" size={20} color={colors.white} style={{ marginRight: spacing.sm }} />
          <Text style={styles.searchButtonText}>조건으로 약물 검색</Text>
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
  resetRow: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginBottom: spacing.md,
  },
  resetText: {
    fontSize: font.sub,
    color: colors.textSub,
    fontWeight: '600',
  },
  scrollContent: {
    padding: spacing.xl,
    paddingBottom: spacing.xxl + spacing.md,
  },
  section: {
    marginBottom: spacing.xl,
  },
  sectionTitle: {
    fontSize: font.body,
    fontWeight: 'bold',
    color: colors.text,
    marginBottom: spacing.md,
  },
  textInput: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 48,
    fontSize: font.body,
    color: colors.text,
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm + 2,
    borderRadius: radius.pill,
    backgroundColor: colors.inputBg,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  chipSelected: {
    backgroundColor: colors.primaryLight,
    borderColor: colors.primary,
  },
  chipText: {
    fontSize: font.sub,
    color: colors.textSub,
  },
  chipTextSelected: {
    color: colors.primaryDark,
    fontWeight: 'bold',
  },
  bottomContainer: {
    padding: spacing.lg,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.white,
  },
  searchButton: {
    backgroundColor: colors.black,
    borderRadius: radius.md,
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchButtonText: {
    color: colors.white,
    fontSize: font.body,
    fontWeight: 'bold',
  },
});

export default PillSearchScreen;
