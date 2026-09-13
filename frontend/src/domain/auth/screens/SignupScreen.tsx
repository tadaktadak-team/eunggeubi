import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
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
import { formatPhone, toDigits } from '../../../shared/utils/phone';
import * as authApi from '../api/auth';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export default function SignupScreen() {
  const navigation = useNavigation<Nav>();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [phone, setPhone] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [name, setName] = useState('');
  const [birth, setBirth] = useState('');
  const [address, setAddress] = useState('');
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeSensitive, setAgreeSensitive] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const sendCode = async () => {
    if (!phone) return Alert.alert('전화번호', '전화번호를 입력해주세요.');
    try {
      await authApi.sendPhoneCode(phone.trim(), 'SIGNUP');
      setCodeSent(true);
      Alert.alert('인증번호 발송', '인증번호를 발송했어요. 문자를 확인해주세요.');
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
    if (!email || !password || !name || !birth) {
      return Alert.alert('입력 확인', '모든 필수 항목을 입력해주세요.');
    }
    if (!phoneVerified) return Alert.alert('휴대폰 인증', '휴대폰 인증을 완료해주세요.');
    const birthDate = toISODate(birth);
    if (!birthDate) return Alert.alert('생년월일', '생년월일 8자리를 정확히 입력해주세요. (예: 19900101)');
    if (!agreeTerms || !agreePrivacy || !agreeSensitive) {
      return Alert.alert('약관 동의', '필수 약관에 동의해주세요.');
    }

    setSubmitting(true);
    try {
      const res = await authApi.signup({
        email: email.trim(),
        password,
        name: name.trim(),
        phone: phone.trim(),
        birthDate,
        gender: 'NONE',
        address: address.trim() || undefined,
        agreeService: agreeTerms,
        agreePrivacy,
        agreeSensitiveInfo: agreeSensitive,
      });
      if (res.guardianConsentRequired) {
        navigation.navigate('GuardianConsent', { userId: res.userId, email: email.trim() });
        return;
      }
      navigation.navigate('SignupComplete', { email: email.trim() });
    } catch (e: any) {
      Alert.alert('회원가입 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={10}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>회원가입</Text>
        <View style={{ width: 26 }} />
      </View>
      <View style={styles.progressTrack}>
        <View style={styles.progressFill} />
      </View>

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          <TextInput
            style={styles.input}
            placeholder="이메일"
            placeholderTextColor={colors.placeholder}
            autoCapitalize="none"
            keyboardType="email-address"
            value={email}
            onChangeText={setEmail}
          />

          <View style={styles.inputWrap}>
            <TextInput
              style={styles.inputFlex}
              placeholder="비밀번호 (8자 이상)"
              placeholderTextColor={colors.placeholder}
              secureTextEntry={!showPassword}
              value={password}
              onChangeText={setPassword}
            />
            <TouchableOpacity onPress={() => setShowPassword((v) => !v)}>
              <Ionicons
                name={showPassword ? 'eye-outline' : 'eye-off-outline'}
                size={20}
                color={colors.placeholder}
              />
            </TouchableOpacity>
          </View>

          <View style={styles.row}>
            <TextInput
              style={[styles.input, styles.rowInput, phoneVerified && styles.inputDisabled]}
              placeholder="010-0000-0000"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              value={formatPhone(phone)}
              onChangeText={(v) => setPhone(toDigits(v))}
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
            placeholder="이름"
            placeholderTextColor={colors.placeholder}
            value={name}
            onChangeText={setName}
          />

          <TextInput
            style={styles.input}
            placeholder="생년월일 (예: 19900101)"
            placeholderTextColor={colors.placeholder}
            keyboardType="number-pad"
            maxLength={8}
            value={birth}
            onChangeText={setBirth}
          />
          <Text style={styles.hint}>만 14세 미만이면 다음 단계에서 보호자 동의가 필요해요</Text>

          <TextInput
            style={styles.input}
            placeholder="주소 (선택)"
            placeholderTextColor={colors.placeholder}
            value={address}
            onChangeText={setAddress}
          />

          <TouchableOpacity style={styles.check} onPress={() => setAgreeTerms((v) => !v)}>
            <Ionicons
              name={agreeTerms ? 'checkbox' : 'square-outline'}
              size={22}
              color={agreeTerms ? colors.primary : colors.placeholder}
            />
            <Text style={styles.checkText}>[필수] 이용약관 동의</Text>
            <TouchableOpacity onPress={() => navigation.navigate('Legal', { tab: 'terms' })} hitSlop={8}>
              <Text style={styles.checkLink}>보기</Text>
            </TouchableOpacity>
          </TouchableOpacity>
          <TouchableOpacity style={styles.check} onPress={() => setAgreePrivacy((v) => !v)}>
            <Ionicons
              name={agreePrivacy ? 'checkbox' : 'square-outline'}
              size={22}
              color={agreePrivacy ? colors.primary : colors.placeholder}
            />
            <Text style={styles.checkText}>[필수] 개인정보 처리 동의</Text>
            <TouchableOpacity onPress={() => navigation.navigate('Legal', { tab: 'privacy' })} hitSlop={8}>
              <Text style={styles.checkLink}>보기</Text>
            </TouchableOpacity>
          </TouchableOpacity>
          <TouchableOpacity style={styles.check} onPress={() => setAgreeSensitive((v) => !v)}>
            <Ionicons
              name={agreeSensitive ? 'checkbox' : 'square-outline'}
              size={22}
              color={agreeSensitive ? colors.primary : colors.placeholder}
            />
            <Text style={styles.checkText}>[필수] 민감정보(건강정보) 처리 동의</Text>
            <TouchableOpacity
              onPress={() => navigation.navigate('Legal', { tab: 'privacy', section: '제4조' })}
              hitSlop={8}
            >
              <Text style={styles.checkLink}>보기</Text>
            </TouchableOpacity>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.nextBtn, submitting && { opacity: 0.6 }]}
            onPress={onSubmit}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.nextBtnText}>다음</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    height: 52,
  },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  progressTrack: { height: 3, backgroundColor: colors.border },
  progressFill: { width: '50%', height: 3, backgroundColor: colors.primary },
  body: { padding: spacing.xl, paddingBottom: spacing.xxl },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    fontSize: font.body,
    color: colors.text,
    marginBottom: spacing.md,
  },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    marginBottom: spacing.md,
  },
  inputFlex: { flex: 1, fontSize: font.body, color: colors.text },
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
  hint: { color: colors.textSub, fontSize: font.caption, marginBottom: spacing.md, marginTop: -spacing.xs },
  check: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm },
  checkText: { flex: 1, marginLeft: spacing.sm, fontSize: font.sub, color: colors.text },
  checkLink: { fontSize: font.caption, color: colors.placeholder, textDecorationLine: 'underline' },
  nextBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  nextBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});