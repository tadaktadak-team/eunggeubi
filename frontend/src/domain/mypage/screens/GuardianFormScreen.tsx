import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { addGuardian, updateGuardian } from '../api/guardian';
import { MyPageStackParamList, Relationship, RELATIONSHIP_LABEL, RELATIONSHIP_OPTIONS } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;
type FormRoute = RouteProp<MyPageStackParamList, 'GuardianForm'>;

export default function GuardianFormScreen() {
  const navigation = useNavigation<Nav>();
  const { params } = useRoute<FormRoute>();
  const editing = params?.guardian; // 있으면 수정 모드

  const [name, setName] = useState(editing?.name ?? '');
  const [phone, setPhone] = useState(editing?.phone ?? '');
  const [relationship, setRelationship] = useState<Relationship>(editing?.relationship ?? 'PARENT');
  const [saving, setSaving] = useState(false);

  const onSave = async () => {
    if (!name.trim() || !phone.trim()) {
      Alert.alert('입력 확인', '이름과 전화번호를 입력해주세요.');
      return;
    }
    try {
      setSaving(true);
      const body = { name: name.trim(), phone: phone.trim(), relationship };
      if (editing) {
        // 수정: 기존 알림 설정은 유지
        await updateGuardian(editing.id, { ...body, notifyEnabled: editing.notifyEnabled });
      } else {
        await addGuardian(body);
      }
      navigation.goBack();
    } catch (e: any) {
      Alert.alert('저장 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={styles.container}>
      <AppHeader title={editing ? '보호자 수정' : '보호자 등록'} />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.label}>이름</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
          placeholder="보호자 이름"
          placeholderTextColor={colors.placeholder}
        />

        <Text style={styles.label}>전화번호</Text>
        <TextInput
          style={styles.input}
          value={phone}
          onChangeText={setPhone}
          placeholder="010-0000-0000"
          placeholderTextColor={colors.placeholder}
          keyboardType="phone-pad"
        />

        <Text style={styles.label}>관계</Text>
        <View style={styles.chipRow}>
          {RELATIONSHIP_OPTIONS.map((r) => {
            const selected = r === relationship;
            return (
              <Pressable
                key={r}
                style={[styles.chip, selected && styles.chipSelected]}
                onPress={() => setRelationship(r)}
              >
                <Text style={[styles.chipText, selected && styles.chipTextSelected]}>
                  {RELATIONSHIP_LABEL[r]}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <Pressable style={[styles.saveBtn, saving && { opacity: 0.6 }]} onPress={onSave} disabled={saving}>
          <Text style={styles.saveText}>{saving ? '저장 중...' : '저장'}</Text>
        </Pressable>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.sm },
  label: { fontSize: font.sub, fontWeight: '700', color: colors.text, marginTop: spacing.md },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 50,
    fontSize: font.body,
    color: colors.text,
  },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm },
  chip: {
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.white,
  },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  chipTextSelected: { color: colors.white },
  saveBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.xl,
  },
  saveText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});