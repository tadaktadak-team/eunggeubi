import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { formatPhone, toDigits } from '../../../shared/utils/phone';
import { sendPhoneCode, verifyPhoneCode } from '../../auth/api/auth';
import { getMyInfo, updateMyInfo } from '../api/user';
import { Gender, MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'NONE', label: '선택 안 함' },
];

const toISODate = (d: string) =>
  /^\d{8}$/.test(d) ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : null;

export default function AccountEditScreen() {
  const navigation = useNavigation<Nav>();

  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  // 전화번호는 아이디 찾기·비밀번호 재설정에 쓰여서, 바꿀 때는 새 번호로 문자 인증을 받아야 저장된다
  const [originalPhone, setOriginalPhone] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');
  const [verifiedPhone, setVerifiedPhone] = useState<string | null>(null);
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
      setPhone(toDigits(info.phone));
      setOriginalPhone(toDigits(info.phone));
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

  const phoneChanged = phone !== originalPhone;
  const phoneVerified = phoneChanged && verifiedPhone === phone;

  const onChangePhone = (v: string) => {
    const digits = toDigits(v);
    if (digits === phone) return;
    setPhone(digits);
    setCodeSent(false);   // 번호가 바뀌면 이전 번호로 받은 인증번호는 쓸 수 없다
    setCode('');
  };

  const sendCode = async () => {
    try {
      await sendPhoneCode(phone, 'SIGNUP');
      setCodeSent(true);
      Alert.alert('인증번호 발송', '새 전화번호로 인증번호를 보냈어요. 문자를 확인해주세요.');
    } catch (e: any) {
      Alert.alert('발송 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const verifyCode = async () => {
    if (!code) return Alert.alert('인증번호', '인증번호를 입력해주세요.');
    try {
      await verifyPhoneCode(phone, 'SIGNUP', code.trim());
      setVerifiedPhone(phone);
    } catch (e: any) {
      Alert.alert('인증 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const onSave = async () => {
    if (!name.trim() || !phone.trim()) {
      Alert.alert('입력 확인', '이름과 전화번호를 입력해주세요.');
      return;
    }
    if (phoneChanged && !phoneVerified) {
      Alert.alert('휴대폰 인증', '바꾼 전화번호로 인증을 완료해주세요.');
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
        phone,
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
        <View style={styles.row}>
          <TextInput
            style={[styles.input, styles.rowInput]}
            value={formatPhone(phone)}
            onChangeText={onChangePhone}
            placeholder="010-0000-0000"
            placeholderTextColor={colors.placeholder}
            keyboardType="phone-pad"
          />
          {phoneChanged && (
            <Pressable
              style={[styles.smallBtn, phoneVerified && styles.smallBtnDone]}
              onPress={sendCode}
              disabled={phoneVerified}
            >
              <Text style={styles.smallBtnText}>{phoneVerified ? '완료' : codeSent ? '재발송' : '인증'}</Text>
            </Pressable>
          )}
        </View>
        {phoneChanged && codeSent && !phoneVerified && (
          <View style={styles.row}>
            <TextInput
              style={[styles.input, styles.rowInput]}
              value={code}
              onChangeText={setCode}
              placeholder="인증번호 6자리"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              maxLength={6}
            />
            <Pressable style={styles.smallBtn} onPress={verifyCode}>
              <Text style={styles.smallBtnText}>확인</Text>
            </Pressable>
          </View>
        )}
        {phoneChanged && (
          <Text style={styles.hint}>
            {phoneVerified
              ? '✓ 인증 완료. 10분 안에 저장해주세요.'
              : '전화번호를 바꾸려면 새 번호로 문자 인증을 받아야 해요.'}
          </Text>
        )}

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
  row: { flexDirection: 'row', gap: spacing.sm },
  rowInput: { flex: 1 },
  smallBtn: {
    backgroundColor: colors.primary,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  smallBtnDone: { backgroundColor: colors.success },
  smallBtnText: { color: colors.white, fontSize: font.sub, fontWeight: '700' },
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
