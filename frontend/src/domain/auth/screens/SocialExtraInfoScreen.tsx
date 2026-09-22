import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useRoute } from '@react-navigation/native';
import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import * as authApi from '../api/auth';
import { completeSocialSignup } from '../api/social';
import { useAuth } from '../hooks/useAuth';
import { Gender } from '../types';

type Rt = RouteProp<RootStackParamList, 'SocialExtraInfo'>;

const GENDERS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'NONE', label: '선택 안 함' },
];

export default function SocialExtraInfoScreen() {
  const { ticket } = useRoute<Rt>().params;
  const { signInWithTokens } = useAuth();

  const [phone, setPhone] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [birth, setBirth] = useState('');
  const [gender, setGender] = useState<Gender>('NONE');
  const [agreeService, setAgreeService] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeSensitive, setAgreeSensitive] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const sendCode = async () => {
    if (!phone) return Alert.alert('전화번호', '전화번호를 입력해주세요.');
    try {
      await authApi.sendPhoneCode(phone.trim(), 'SIGNUP');
      setCodeSent(true);
      Alert.alert('인증번호 발송', '인증번호를 발송했어요.\n(개발 중: 서버 콘솔에서 확인)');
    } catch (e: any) {
      Alert.alert('발송 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const verifyCode = async () => {
    if (!code) return Alert.alert('인증번호', '인증번호를 입력해주세요.');
    try {
      await authApi.verifyPhoneCode(phone.trim(), 'SIGNUP', code.trim());
      setPhoneVerified(true);
      Alert.alert('인증 완료', '휴대폰 인증이 완료됐어요.');
    } catch (e: any) {
      Alert.alert('인증 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const toISODate = (d: string) =>
    /^\d{8}$/.test(d) ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : null;

  const onSubmit = async () => {
    if (!phoneVerified) return Alert.alert('휴대폰 인증', '휴대폰 인증을 완료해주세요.');
    const birthDate = toISODate(birth);
    if (!birthDate) return Alert.alert('생년월일', '생년월일 8자리를 정확히 입력해주세요. (예: 19900101)');
    if (!agreeService || !agreePrivacy || !agreeSensitive) {
      return Alert.alert('약관 동의', '필수 약관에 모두 동의해주세요.');
    }

    setSubmitting(true);
    try {
      const res = await completeSocialSignup(
        ticket,
        { agreeService, agreePrivacy, agreeSensitiveInfo: agreeSensitive },
        { phone: phone.trim(), birthDate, gender },
      );
      await signInWithTokens(res);
      // 성공하면 AuthProvider가 자동으로 홈으로 전환됨
    } catch (e: any) {
      Alert.alert('가입 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>추가 정보 입력</Text>
          <Text style={styles.subtitle}>가입을 마치려면 아래 정보가 필요해요.</Text>

          <View style={styles.row}>
            <TextInput
              style={[styles.input, styles.rowInput, phoneVerified && styles.inputDisabled]}
              placeholder="전화번호 ('-' 없이)"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              value={phone}
              onChangeText={setPhone}
              editable={!phoneVerified}
            />
            <TouchableOpacity
              style={[styles.smallBtn, phoneVerified && styles.smallBtnDone]}
              onPress={sendCode}
              disabled={phoneVerified}
            >
              <Text style={styles.smallBtnText}>{phoneVerified ? '완료' : '인증'}</Text>
            </TouchableOpacity>
          </View>

          {codeSent && !phoneVerified && (
            <View style={styles.row}>
              <TextInput
                style={[styles.input, styles.rowInput]}
                placeholder="인증번호 6자리"
                placeholderTextColor={colors.placeholder}
                keyboardType="number-pad"
                value={code}
                onChangeText={setCode}
                maxLength={6}
              />
              <TouchableOpacity style={styles.smallBtn} onPress={verifyCode}>
                <Text style={styles.smallBtnText}>확인</Text>
              </TouchableOpacity>
            </View>
          )}
          {phoneVerified && <Text style={styles.verifiedText}>✓ 휴대폰 인증 완료</Text>}

          <TextInput
            style={styles.input}
            placeholder="생년월일 (예: 19900101)"
            placeholderTextColor={colors.placeholder}
            keyboardType="number-pad"
            maxLength={8}
            value={birth}
            onChangeText={setBirth}
          />

          <View style={styles.genderRow}>
            {GENDERS.map((g) => (
              <TouchableOpacity
                key={g.value}
                style={[styles.genderBtn, gender === g.value && styles.genderBtnActive]}
                onPress={() => setGender(g.value)}
              >
                <Text style={[styles.genderText, gender === g.value && styles.genderTextActive]}>
                  {g.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity style={styles.check} onPress={() => setAgreeService((v) => !v)}>
            <Ionicons name={agreeService ? 'checkbox' : 'square-outline'} size={22} color={agreeService ? colors.primary : colors.placeholder} />
            <Text style={styles.checkText}>[필수] 이용약관 동의</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.check} onPress={() => setAgreePrivacy((v) => !v)}>
            <Ionicons name={agreePrivacy ? 'checkbox' : 'square-outline'} size={22} color={agreePrivacy ? colors.primary : colors.placeholder} />
            <Text style={styles.checkText}>[필수] 개인정보 처리 동의</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.check} onPress={() => setAgreeSensitive((v) => !v)}>
            <Ionicons name={agreeSensitive ? 'checkbox' : 'square-outline'} size={22} color={agreeSensitive ? colors.primary : colors.placeholder} />
            <Text style={styles.checkText}>[필수] 민감정보 처리 동의</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.btn, submitting && { opacity: 0.6 }]}
            onPress={onSubmit}
            disabled={submitting}
          >
            {submitting ? <ActivityIndicator color={colors.white} /> : <Text style={styles.btnText}>가입 완료</Text>}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  flex: { flex: 1 },
  body: { padding: spacing.xl, paddingBottom: spacing.xxl },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, marginBottom: spacing.sm },
  subtitle: { fontSize: font.body, color: colors.textSub, marginBottom: spacing.xl },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    fontSize: font.body,
    color: colors.text,
    marginBottom: spacing.md,
  },
  inputDisabled: { opacity: 0.6 },
  row: { flexDirection: 'row', alignItems: 'center', marginBottom: spacing.md },
  rowInput: { flex: 1, marginBottom: 0, marginRight: spacing.sm },
  smallBtn: {
    backgroundColor: colors.text,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  smallBtnDone: { backgroundColor: colors.success },
  smallBtnText: { color: colors.white, fontSize: font.sub, fontWeight: '700' },
  verifiedText: { color: colors.success, fontSize: font.sub, marginBottom: spacing.md },
  genderRow: { flexDirection: 'row', marginBottom: spacing.md },
  genderBtn: {
    flex: 1,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  genderBtnActive: { backgroundColor: colors.primary },
  genderText: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  genderTextActive: { color: colors.white },
  check: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm },
  checkText: { marginLeft: spacing.sm, fontSize: font.sub, color: colors.text },
  btn: {
    marginTop: spacing.lg,
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});