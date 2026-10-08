import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Dispatch, SetStateAction, useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getHealthProfile, saveHealthProfile } from '../api/health';
import { BLOOD_TYPES, MedicationItem, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;
type HealthRoute = RouteProp<MyPageStackParamList, 'Health'>;

// 같은 약: 번호가 있으면 번호로, 직접 입력한 약은 이름으로 비교
const sameMedication = (a: MedicationItem, b: MedicationItem) =>
  a.itemSeq && b.itemSeq ? a.itemSeq === b.itemSeq : a.name === b.name;

// 태그(칩) 추가/삭제 섹션 (지병·알레르기·복약 공통 재사용)
function TagSection({
  title,
  items,
  onAdd,
  onRemove,
}: {
  title: string;
  items: string[];
  onAdd: (v: string) => void;
  onRemove: (v: string) => void;
}) {
  const [input, setInput] = useState('');
  // 서버는 콤마로 이어 저장하므로 "땅콩, 호두"는 두 항목으로 나눠 넣는다(저장 후 다시 열어도 모양이 같게)
  const add = () => {
    const values = input.split(',').map((v) => v.trim()).filter(Boolean);
    if (values.length === 0) return;
    values.forEach(onAdd);
    setInput('');
  };
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      <View style={styles.chipWrap}>
        {items.map((it) => (
          <Pressable key={it} style={styles.tag} onPress={() => onRemove(it)}>
            <Text style={styles.tagText}>{it}</Text>
            <Ionicons name="close" size={14} color={colors.primaryDark} />
          </Pressable>
        ))}
        {items.length === 0 && <Text style={styles.empty}>등록된 항목이 없습니다</Text>}
      </View>
      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          placeholder={`${title} 입력 후 추가`}
          placeholderTextColor={colors.placeholder}
          maxLength={50}
          onSubmitEditing={add}
          returnKeyType="done"
        />
        <Pressable style={styles.smallBtn} onPress={add}>
          <Text style={styles.smallBtnText}>추가</Text>
        </Pressable>
      </View>
    </View>
  );
}

// 복용약: 약 검색 화면에서 골라 추가한다 (번호가 연결된 약은 파란 칩, 직접 입력한 약은 회색 칩)
function MedicationSection({
  items,
  onRemove,
  onSearch,
}: {
  items: MedicationItem[];
  onRemove: (item: MedicationItem) => void;
  onSearch: () => void;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>복용 중인 약</Text>
      <View style={styles.chipWrap}>
        {items.map((it) => (
          <Pressable
            key={it.itemSeq ?? `name:${it.name}`}
            style={[styles.tag, !it.itemSeq && styles.tagUnlinked]}
            onPress={() => onRemove(it)}
          >
            <Text style={[styles.tagText, !it.itemSeq && styles.tagTextUnlinked]} numberOfLines={1}>
              {it.name}
            </Text>
            <Ionicons name="close" size={14} color={it.itemSeq ? colors.primaryDark : colors.textSub} />
          </Pressable>
        ))}
        {items.length === 0 && <Text style={styles.empty}>등록된 항목이 없습니다</Text>}
      </View>
      <Pressable style={styles.searchBtn} onPress={onSearch}>
        <Ionicons name="search" size={16} color={colors.textSub} />
        <Text style={styles.searchBtnText}>약 검색해서 추가</Text>
      </Pressable>
    </View>
  );
}

export default function HealthScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<HealthRoute>();
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [saving, setSaving] = useState(false);
  const [bloodType, setBloodType] = useState<string | null>(null);
  const [diseases, setDiseases] = useState<string[]>([]);
  const [medications, setMedications] = useState<MedicationItem[]>([]);
  const [allergies, setAllergies] = useState<string[]>([]);

  // 불러오기에 실패한 채 빈 화면으로 저장하면 기존 프로필이 지워지므로, 실패하면 저장 대신 다시 시도만 보여준다
  useEffect(() => {
    (async () => {
      try {
        const p = await getHealthProfile();
        setBloodType(p.bloodType);
        setDiseases(p.diseases);
        setMedications(p.medicationItems ?? p.medications.map((name) => ({ name, itemSeq: null })));
        setAllergies(p.allergies);
        setLoadFailed(false);
      } catch (e: any) {
        console.error('건강 프로필 조회 실패:', e);
        setLoadFailed(true);
      } finally {
        setLoading(false);
      }
    })();
  }, [attempt]);

  const retry = () => {
    setLoading(true);
    setAttempt((n) => n + 1);
  };

  // 약 검색 화면에서 고른 약을 받아 목록에 추가
  useFocusEffect(
    useCallback(() => {
      const picked = route.params?.pickedMedication;
      if (!picked) return;
      setMedications((prev) => (prev.some((m) => sameMedication(m, picked)) ? prev : [...prev, picked]));
      navigation.setParams({ pickedMedication: undefined });
    }, [route.params?.pickedMedication, navigation]),
  );

  const addTo = (setter: Dispatch<SetStateAction<string[]>>) => (v: string) =>
    setter((prev) => (prev.includes(v) ? prev : [...prev, v]));
  const removeFrom = (setter: Dispatch<SetStateAction<string[]>>) => (v: string) =>
    setter((prev) => prev.filter((x) => x !== v));

  const onSave = async () => {
    try {
      setSaving(true);
      await saveHealthProfile({ bloodType, diseases, medicationItems: medications, allergies });
      Alert.alert('저장 완료', '건강 프로필이 저장됐어요.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ]);
    } catch (e: any) {
      Alert.alert('저장 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <AppHeader title="건강 프로필" />
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      </View>
    );
  }

  if (loadFailed) {
    return (
      <View style={styles.container}>
        <AppHeader title="건강 프로필" />
        <View style={styles.failBox}>
          <Text style={styles.failText}>건강 프로필을 불러오지 못했어요.{'\n'}네트워크를 확인하고 다시 시도해주세요.</Text>
          <Pressable style={styles.retryBtn} onPress={retry}>
            <Text style={styles.saveText}>다시 시도</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <AppHeader title="건강 프로필" />
      <ScrollView contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets
      >

        {/* 혈액형 (단일 선택) */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>혈액형</Text>
          <View style={styles.chipWrap}>
            {BLOOD_TYPES.map((bt) => {
              const selected = bt === bloodType;
              return (
                <Pressable
                  key={bt}
                  style={[styles.selectChip, selected && styles.selectChipOn]}
                  onPress={() => setBloodType(selected ? null : bt)}
                >
                  <Text style={[styles.selectChipText, selected && styles.selectChipTextOn]}>{bt}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        <TagSection title="지병" items={diseases} onAdd={addTo(setDiseases)} onRemove={removeFrom(setDiseases)} />
        <MedicationSection
          items={medications}
          onRemove={(item) => setMedications((prev) => prev.filter((m) => !sameMedication(m, item)))}
          onSearch={() => navigation.navigate('MedicationPicker')}
        />
        <TagSection title="알레르기" items={allergies} onAdd={addTo(setAllergies)} onRemove={removeFrom(setAllergies)} />

        <Pressable style={[styles.saveBtn, saving && { opacity: 0.6 }]} onPress={onSave} disabled={saving}>
          <Text style={styles.saveText}>{saving ? '저장 중...' : '저장'}</Text>
        </Pressable>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.lg },
  summary: { backgroundColor: colors.primaryLight, borderRadius: radius.md, padding: spacing.lg, gap: spacing.sm },
  summaryLabel: { fontSize: font.sub, color: colors.primaryDark, fontWeight: '700' },
  summaryChip: { backgroundColor: colors.white, borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: 4 },
  summaryChipText: { fontSize: font.caption, color: colors.primaryDark, fontWeight: '600' },
  section: { gap: spacing.sm },
  sectionTitle: { fontSize: font.body, fontWeight: '700', color: colors.text },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, alignItems: 'center' },
  empty: { fontSize: font.sub, color: colors.placeholder },
  tag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    backgroundColor: colors.primaryLight,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: 4,
  },
  tagText: { fontSize: font.sub, color: colors.primaryDark, fontWeight: '600', flexShrink: 1 },
  tagUnlinked: { backgroundColor: colors.inputBg },
  tagTextUnlinked: { color: colors.textSub },
  searchBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    height: 46,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: colors.border,
    borderRadius: radius.md,
  },
  searchBtnText: { color: colors.textSub, fontSize: font.sub, fontWeight: '600' },
  selectChip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.white,
  },
  selectChipOn: { backgroundColor: colors.primary, borderColor: colors.primary },
  selectChipText: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  selectChipTextOn: { color: colors.white },
  inputRow: { flexDirection: 'row', gap: spacing.sm },
  input: {
    flex: 1,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 46,
    fontSize: font.body,
    color: colors.text,
  },
  smallBtn: {
    paddingHorizontal: spacing.lg,
    borderRadius: radius.md,
    backgroundColor: colors.text,
    alignItems: 'center',
    justifyContent: 'center',
  },
  smallBtnText: { color: colors.white, fontSize: font.sub, fontWeight: '700' },
  saveBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.sm,
  },
  saveText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  failBox: { alignItems: 'center', gap: spacing.lg, padding: spacing.xl, marginTop: spacing.xxl },
  failText: { fontSize: font.body, color: colors.textSub, textAlign: 'center', lineHeight: 22 },
  retryBtn: {
    backgroundColor: colors.primary,
    height: 48,
    paddingHorizontal: spacing.xl,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
});