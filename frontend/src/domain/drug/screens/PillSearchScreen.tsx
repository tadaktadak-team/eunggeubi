import React, { useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from 'react-native';
import { Feather, Ionicons } from '@expo/vector-icons';

const PillSearchScreen = ({ navigation }: any) => {
  const [printText, setPrintText] = useState('');
  const [selectedShape, setSelectedShape] = useState('');
  const [selectedColor, setSelectedColor] = useState('');
  const [selectedForm, setSelectedForm] = useState('');

  // 필터 옵션 데이터
  const shapes = ['원형', '타원형', '장방형', '삼각형', '사각형', '기타'];
  const colors = ['하양', '노랑', '주황', '분홍', '빨강', '갈색', '연두', '초록', '파랑'];
  const forms = ['정제', '경질캡슐', '연질캡슐'];

  const handleReset = () => {
    setPrintText('');
    setSelectedShape('');
    setSelectedColor('');
    setSelectedForm('');
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* 상단 헤더 (뒤로가기 버튼) */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Feather name="arrow-left" size={24} color="#111111" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>낱알 특징 검색</Text>
        <TouchableOpacity onPress={handleReset}>
          <Text style={styles.resetText}>초기화</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* 1. 식별문자 입력 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>식별문자</Text>
          <TextInput
            style={styles.textInput}
            placeholder="알약에 적힌 글자 (예: TY, 500)"
            value={printText}
            onChangeText={setPrintText}
            placeholderTextColor="#A0A0A0"
          />
        </View>

        {/* 2. 모양 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>모양</Text>
          <View style={styles.chipContainer}>
            {shapes.map((shape) => (
              <TouchableOpacity
                key={shape}
                style={[
                  styles.chip,
                  selectedShape === shape && styles.chipSelected,
                ]}
                onPress={() => setSelectedShape(selectedShape === shape ? '' : shape)}
              >
                <Text
                  style={[
                    styles.chipText,
                    selectedShape === shape && styles.chipTextSelected,
                  ]}
                >
                  {shape}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 3. 색상 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>색상</Text>
          <View style={styles.chipContainer}>
            {colors.map((color) => (
              <TouchableOpacity
                key={color}
                style={[
                  styles.chip,
                  selectedColor === color && styles.chipSelected,
                ]}
                onPress={() => setSelectedColor(selectedColor === color ? '' : color)}
              >
                <Text
                  style={[
                    styles.chipText,
                    selectedColor === color && styles.chipTextSelected,
                  ]}
                >
                  {color}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 4. 제형 선택 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>제형</Text>
          <View style={styles.chipContainer}>
            {forms.map((form) => (
              <TouchableOpacity
                key={form}
                style={[
                  styles.chip,
                  selectedForm === form && styles.chipSelected,
                ]}
                onPress={() => setSelectedForm(selectedForm === form ? '' : form)}
              >
                <Text
                  style={[
                    styles.chipText,
                    selectedForm === form && styles.chipTextSelected,
                  ]}
                >
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
          <Ionicons name="search" size={20} color="#FFFFFF" style={{ marginRight: 6 }} />
          <Text style={styles.searchButtonText}>조건으로 약물 검색</Text>
        </TouchableOpacity>
      </View>
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
  resetText: {
    fontSize: 14,
    color: '#8E8E93',
  },
  scrollContent: {
    padding: 20,
    paddingBottom: 40,
  },
  section: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#111111',
    marginBottom: 12,
  },
  textInput: {
    backgroundColor: '#F5F5F5',
    borderRadius: 12,
    paddingHorizontal: 16,
    height: 48,
    fontSize: 15,
    color: '#111111',
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    backgroundColor: '#F5F5F5',
    borderWidth: 1,
    borderColor: 'transparent',
  },
  chipSelected: {
    backgroundColor: '#EBF3FF',
    borderColor: '#0066FF',
  },
  chipText: {
    fontSize: 14,
    color: '#555555',
  },
  chipTextSelected: {
    color: '#0066FF',
    fontWeight: 'bold',
  },
  bottomContainer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
    backgroundColor: '#FFFFFF',
  },
  searchButton: {
    backgroundColor: '#111111',
    borderRadius: 14,
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default PillSearchScreen;