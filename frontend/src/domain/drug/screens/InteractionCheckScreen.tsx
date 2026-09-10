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
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';

const InteractionCheckScreen = ({ navigation }: any) => {
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
    <SafeAreaView style={styles.container}>
      {/* 상단 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Feather name="arrow-left" size={24} color="#111111" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>상호작용 체크</Text>
        <View style={{ width: 24 }} />
      </View>

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
            placeholderTextColor="#A0A0A0"
          />
          <TouchableOpacity style={styles.addButton} onPress={handleAddDrug}>
            <Feather name="plus" size={20} color="#FFFFFF" />
          </TouchableOpacity>
        </View>

        {/* 선택된 약물 태그 리스트 */}
        <View style={styles.drugListSection}>
          <Text style={styles.sectionLabel}>선택된 약물 ({selectedDrugs.length})</Text>
          <View style={styles.chipWrapper}>
            {selectedDrugs.map((drug, index) => (
              <View key={index} style={styles.drugChip}>
                <MaterialCommunityIcons name="pill" size={16} color="#E53935" style={{ marginRight: 6 }} />
                <Text style={styles.chipText}>{drug}</Text>
                <TouchableOpacity onPress={() => handleRemoveDrug(index)} style={styles.removeIcon}>
                  <Feather name="x" size={16} color="#8E8E93" />
                </TouchableOpacity>
              </View>
            ))}
          </View>
        </View>

        {/* 상호작용 검사 결과 출력 영역 */}
        {hasChecked && (
          <View style={styles.resultSection}>
            <View style={styles.resultHeader}>
              <Ionicons name="warning-outline" size={22} color="#D32F2F" />
              <Text style={styles.resultTitle}>병용 주의 필요</Text>
            </View>
            <Text style={styles.resultDesc}>
              <Text style={styles.boldText}>'타이레놀정'</Text>과 <Text style={styles.boldText}>'아스피린정'</Text>을 함께 복용 시 위장관 출혈 위험이 증가할 수 있습니다. 전문가(의사/약사)와 상의 후 복용을 권장합니다.
            </Text>
          </View>
        )}
      </ScrollView>

      {/* 하단 검사하기 버튼 */}
      <View style={styles.bottomContainer}>
        <TouchableOpacity style={styles.checkButton} onPress={handleCheck} activeOpacity={0.8}>
          <Ionicons name="pulse-outline" size={20} color="#FFFFFF" style={{ marginRight: 6 }} />
          <Text style={styles.checkButtonText}>상호작용 확인하기</Text>
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
  scrollContent: {
    padding: 20,
  },
  subTitle: {
    fontSize: 14,
    color: '#666666',
    marginBottom: 20,
    lineHeight: 20,
  },
  inputContainer: {
    flexDirection: 'row',
    marginBottom: 24,
  },
  input: {
    flex: 1,
    backgroundColor: '#F5F5F5',
    borderRadius: 12,
    paddingHorizontal: 16,
    height: 48,
    fontSize: 15,
    color: '#111111',
    marginRight: 8,
  },
  addButton: {
    width: 48,
    height: 48,
    backgroundColor: '#111111',
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  drugListSection: {
    marginBottom: 24,
  },
  sectionLabel: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#8E8E93',
    marginBottom: 12,
  },
  chipWrapper: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  drugChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
  },
  chipText: {
    fontSize: 14,
    color: '#111111',
    fontWeight: '500',
  },
  removeIcon: {
    marginLeft: 6,
    padding: 2,
  },
  resultSection: {
    backgroundColor: '#FFF8F8',
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#FFEBEE',
  },
  resultHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
    gap: 6,
  },
  resultTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#D32F2F',
  },
  resultDesc: {
    fontSize: 14,
    lineHeight: 22,
    color: '#444444',
  },
  boldText: {
    fontWeight: 'bold',
    color: '#111111',
  },
  bottomContainer: {
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
    backgroundColor: '#FFFFFF',
  },
  checkButton: {
    backgroundColor: '#111111',
    borderRadius: 14,
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default InteractionCheckScreen;