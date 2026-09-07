import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { getMyInfo, updateMyInfo } from '../api/user';
import { Gender, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'NONE', label: '선택 안 함' },
];

const toDigits = (v: string) => v.replace(/\D/g, '');
const toISODate = (d: string) =>
  /^\d{8}$/.test(d) ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : null;

export default function AccountEditScreen() {
  const navigation = useNavigation<Nav>();

  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [birth, setBirth] = useState('');
  const [gender, setGender] = useState<Gender>('NONE');
  const [address, setAddress] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const info = await getMyInfo();
      setEmail(info.email);
      setName(info.name);
      setPhone(info.phone);
      setBirth(toDigits(info.birthDate));
      setGender(info.gender);
      setAddress(info.address ?? '');
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '내 정보를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onSave = async () => {
    if (!name.trim() || !phone.trim()) {
      Alert.alert('입력 확인', '이름과 전화번호를 입력해주세요.');
      return;
    }
    const birthDate = toISODate(birth);
    if (!birthDate) {
      Alert.alert('생년월일', '생년월일 8자리를 정확히 입력해주세요. (예: 19900101)');
      return;
    }

    try {
      setSaving(true);
      await updateMyInfo({
        name: name.trim(),
        phone: phone.trim(),
        birthDate,
        gender,
        address: address.trim() || null,
      });
      Alert.alert('저장 완료', '회원 정보가 수정됐어요.', [
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
        <AppHeader title="계정 정보" />
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <AppHeader title="계정 정보" />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets
      >
        <Text style={styles.label}>이메일</Text>
        <View style={styles.readonly}>
          <Text style={styles.readonlyText}>{email}</Text>
        </View>
        <Text style={styles.hint}>이메일은 변경할 수 없어요</Text>

        <Text style={styles.label}>이름</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
          placeholder="이름"
          placeholderTextColor={colors.placeholder}
        />

        <Text style={styles.label}>전화번호</Text>
        <TextInput
          style={styles.input}
          value={phone}
          onChangeText={setPhone}
          placeholder="01000000000"
          placeholderTextColor={colors.placeholder}
          keyboardType="phone-pad"
        />

        <Text style={styles.label}>생년월일</Text>
        <TextInput
          style={styles.input}
          value={birth}
          onChangeText={(v) => setBirth(toDigits(v))}
          placeholder="19900101"
          placeholderTextColor={colors.placeholder}
          keyboardType="number-pad"
          maxLength={8}
        />

        <Text style={styles.label}>성별</Text>
        <View style={styles.chipRow}>
          {GENDER_OPTIONS.map((g) => {
            const selected = g.value === gender;
            return (
              <Pressable
                key={g.value}
                style={[styles.chip, selected && styles.chipSelected]}
                onPress={() => setGender(g.value)}
              >
                <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{g.label}</Text>
              </Pressable>
            );
          })}
        </View>

        <Text style={styles.label}>주소</Text>
        <TextInput
          style={styles.input}
          value={address}
          onChangeText={setAddress}
          placeholder="주소 (선택)"
          placeholderTextColor={colors.placeholder}
        />

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
  readonly: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 50,
    justifyContent: 'center',
  },
  readonlyText: { fontSize: font.body, color: colors.placeholder },
  hint: { fontSize: font.caption, color: colors.placeholder },
  chipRow: { flexDirection: 'row', gap: spacing.sm },
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
