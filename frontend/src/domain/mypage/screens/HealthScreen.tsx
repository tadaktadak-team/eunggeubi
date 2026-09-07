import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Dispatch, SetStateAction, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getHealthProfile, saveHealthProfile } from '../api/health';
import { BLOOD_TYPES, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

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
  const add = () => {
    const v = input.trim();
    if (!v) return;
    onAdd(v);
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

export default function HealthScreen() {
  const navigation = useNavigation<Nav>();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [bloodType, setBloodType] = useState<string | null>(null);
  const [diseases, setDiseases] = useState<string[]>([]);
  const [medications, setMedications] = useState<string[]>([]);
  const [allergies, setAllergies] = useState<string[]>([]);

  useEffect(() => {
    (async () => {
      try {
        const p = await getHealthProfile();
        setBloodType(p.bloodType);
        setDiseases(p.diseases);
        setMedications(p.medications);
        setAllergies(p.allergies);
      } catch (e: any) {
        Alert.alert('오류', e?.message ?? '불러오지 못했어요.');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const addTo = (setter: Dispatch<SetStateAction<string[]>>) => (v: string) =>
    setter((prev) => (prev.includes(v) ? prev : [...prev, v]));
  const removeFrom = (setter: Dispatch<SetStateAction<string[]>>) => (v: string) =>
    setter((prev) => prev.filter((x) => x !== v));

  const onSave = async () => {
    try {
      setSaving(true);
      await saveHealthProfile({ bloodType, diseases, medications, allergies });
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
        <TagSection title="복용 중인 약" items={medications} onAdd={addTo(setMedications)} onRemove={removeFrom(setMedications)} />
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
  tagText: { fontSize: font.sub, color: colors.primaryDark, fontWeight: '600' },
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
});