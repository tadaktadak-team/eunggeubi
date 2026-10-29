import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import {
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
import { showAlert } from '../../../shared/dialog';
import Button from '../../../shared/components/Button';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { formatPhone, toDigits } from '../../../shared/utils/phone';
import { PASSWORD_RULE_TEXT, validatePassword } from '../../../shared/utils/password';
import * as authApi from '../api/auth';

type Nav = NativeStackNavigationProp<RootStackParamList>;

// 입력 검증 결과는 모달이 아니라 해당 칸 아래에 띄운다 - 모달은 어느 칸이 틀렸는지 알려주지 못하고,
// 확인을 눌러 닫아야 화면이 보이며, 틀린 칸이 여러 개면 그 수만큼 반복된다.
type FieldErrors = {
  email?: string;
  password?: string;
  passwordConfirm?: string;
  phone?: string;
  code?: string;
  name?: string;
  birth?: string;
  agree?: string;
};

export default function SignupScreen() {
  const navigation = useNavigation<Nav>();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [passwordConfirm, setPasswordConfirm] = useState('');
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
  const [errors, setErrors] = useState<FieldErrors>({});
  const [codeNotice, setCodeNotice] = useState('');

  // 사용자가 그 칸을 고치기 시작하면 해당 에러만 지운다 - 다른 칸의 에러는 그대로 둬야
  // "아직 남은 게 무엇인지"가 계속 보인다.
  const clearError = (field: keyof FieldErrors) =>
    setErrors((prev) => (prev[field] === undefined ? prev : { ...prev, [field]: undefined }));

  const setError = (field: keyof FieldErrors, message: string) =>
    setErrors((prev) => ({ ...prev, [field]: message }));

  const sendCode = async () => {
    if (!phone.trim()) return setError('phone', '전화번호를 입력해주세요');
    clearError('phone');
    setCodeNotice('');
    try {
      await authApi.sendPhoneCode(phone.trim(), 'SIGNUP');
      setCodeSent(true);
      setCodeNotice('인증번호를 보냈어요. 문자를 확인해주세요');
    } catch (e: any) {
      setError('phone', e?.message ?? '발송에 실패했어요. 잠시 후 다시 시도해주세요');
    }
  };

  const verifyCode = async () => {
    if (!code.trim()) return setError('code', '인증번호를 입력해주세요');
    clearError('code');
    try {
      await authApi.verifyPhoneCode(phone.trim(), 'SIGNUP', code.trim());
      setPhoneVerified(true);
      setCodeNotice('');
      clearError('phone');
    } catch (e: any) {
      setError('code', e?.message ?? '인증번호가 맞지 않아요');
    }
  };

  const toISODate = (d: string) =>
    /^\d{8}$/.test(d) ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : null;

  // 첫 번째 오류에서 멈추지 않고 전부 모아서 돌려준다 - 한 번 누르면 남은 문제를 한눈에 보게 한다.
  const collectErrors = (): FieldErrors => {
    const found: FieldErrors = {};

    if (!email.trim()) found.email = '이메일을 입력해주세요';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) found.email = '이메일 형식이 올바르지 않아요';

    if (!password) {
      found.password = '비밀번호를 입력해주세요';
    } else {
      const pwCheck = validatePassword(password, { email: email.trim(), phone: phone.trim() });
      if (!pwCheck.ok) found.password = pwCheck.message;
    }

    if (!passwordConfirm) found.passwordConfirm = '비밀번호를 한 번 더 입력해주세요';
    else if (password !== passwordConfirm) found.passwordConfirm = '비밀번호가 일치하지 않아요';

    if (!phone.trim()) found.phone = '전화번호를 입력해주세요';
    else if (!phoneVerified) found.phone = '휴대폰 인증을 완료해주세요';

    if (!name.trim()) found.name = '이름을 입력해주세요';

    if (!birth) found.birth = '생년월일을 입력해주세요';
    else if (!toISODate(birth)) found.birth = '8자리를 정확히 입력해주세요 (예: 19900101)';

    if (!agreeTerms || !agreePrivacy || !agreeSensitive) found.agree = '필수 약관에 모두 동의해주세요';

    return found;
  };

  const onSubmit = async () => {
    const found = collectErrors();
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    const birthDate = toISODate(birth)!;

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
        navigation.navigate('GuardianConsent', {
          consentToken: res.consentToken!,
          email: email.trim(),
        });

        return;
      }
      navigation.navigate('SignupComplete', { email: email.trim() });
    } catch (e: any) {
      const msg = e?.message ?? '다시 시도해주세요.';

      // 이미 가입된 이메일이면 → 로그인 / 비밀번호 찾기로 유도
      if (msg.includes('이미 가입')) {
        showAlert('이미 가입된 이메일', '이 이메일로 가입된 계정이 있어요.', [
          { text: '로그인하기', onPress: () => navigation.navigate('Login') },
          {
            text: '비밀번호 찾기',
            onPress: () => navigation.navigate('FindAccount', { tab: 'FIND_PW' }),
          },
          { text: '취소', style: 'cancel' },
        ]);
        return;
      }

      showAlert('회원가입 실패', msg);
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
            style={[styles.input, errors.email && styles.inputError]}
            placeholder="이메일"
            placeholderTextColor={colors.placeholder}
            autoCapitalize="none"
            keyboardType="email-address"
            value={email}
            onChangeText={(v) => {
              setEmail(v);
              clearError('email');
            }}
          />
          {errors.email && <Text style={styles.error}>{errors.email}</Text>}

          <View style={[styles.inputWrap, errors.password && styles.inputError]}>
            <TextInput
              style={styles.inputFlex}
              placeholder="비밀번호"
              placeholderTextColor={colors.placeholder}
              secureTextEntry={!showPassword}
              value={password}
              onChangeText={(v) => {
                setPassword(v);
                clearError('password');
              }}
            />
            <TouchableOpacity onPress={() => setShowPassword((v) => !v)}>
              <Ionicons
                name={showPassword ? 'eye-outline' : 'eye-off-outline'}
                size={20}
                color={colors.placeholder}
              />
            </TouchableOpacity>
          </View>
          {errors.password ? (
            <Text style={styles.error}>{errors.password}</Text>
          ) : (
            <Text style={styles.hint}>{PASSWORD_RULE_TEXT}</Text>
          )}

          <TextInput
            style={[styles.input, errors.passwordConfirm && styles.inputError]}
            placeholder="비밀번호 확인"
            placeholderTextColor={colors.placeholder}
            secureTextEntry={!showPassword}
            value={passwordConfirm}
            onChangeText={(v) => {
              setPasswordConfirm(v);
              clearError('passwordConfirm');
            }}
          />
          {errors.passwordConfirm && <Text style={styles.error}>{errors.passwordConfirm}</Text>}

          <View style={styles.row}>
            <TextInput
              style={[
                styles.input,
                styles.rowInput,
                phoneVerified && styles.inputDisabled,
                errors.phone && styles.inputError,
              ]}
              placeholder="010-0000-0000"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              value={formatPhone(phone)}
              onChangeText={(v) => {
                setPhone(toDigits(v));
                clearError('phone');
              }}
              editable={!phoneVerified}
            />
            <Button
              title={phoneVerified ? '완료' : '인증'}
              variant={phoneVerified ? 'success' : 'neutral'}
              onPress={sendCode}
              done={phoneVerified}
              textStyle={styles.smallBtnText}
            />
          </View>
          {errors.phone && <Text style={styles.error}>{errors.phone}</Text>}
          {!errors.phone && !!codeNotice && <Text style={styles.notice}>{codeNotice}</Text>}

          {codeSent && !phoneVerified && (
            <>
              <View style={styles.row}>
                <TextInput
                  style={[styles.input, styles.rowInput, errors.code && styles.inputError]}
                  placeholder="인증번호 6자리"
                  placeholderTextColor={colors.placeholder}
                  keyboardType="number-pad"
                  textContentType="oneTimeCode"
                  value={code}
                  onChangeText={(v) => {
                    setCode(v);
                    clearError('code');
                  }}
                  maxLength={6}
                />
                <Button
                  title="확인"
                  variant="neutral"
                  onPress={verifyCode}
                  textStyle={styles.smallBtnText}
                />
              </View>
              {errors.code && <Text style={styles.error}>{errors.code}</Text>}
            </>
          )}
          {phoneVerified && <Text style={styles.verifiedText}>✓ 휴대폰 인증 완료</Text>}

          <TextInput
            style={[styles.input, errors.name && styles.inputError]}
            placeholder="이름"
            placeholderTextColor={colors.placeholder}
            value={name}
            onChangeText={(v) => {
              setName(v);
              clearError('name');
            }}
          />
          {errors.name && <Text style={styles.error}>{errors.name}</Text>}

          <TextInput
            style={[styles.input, errors.birth && styles.inputError]}
            placeholder="생년월일 (예: 19900101)"
            placeholderTextColor={colors.placeholder}
            keyboardType="number-pad"
            maxLength={8}
            value={birth}
            onChangeText={(v) => {
              setBirth(v);
              clearError('birth');
            }}
          />
          {errors.birth ? (
            <Text style={styles.error}>{errors.birth}</Text>
          ) : (
            <Text style={styles.hint}>만 14세 미만이면 다음 단계에서 보호자 동의가 필요해요</Text>
          )}

          <TextInput
            style={styles.input}
            placeholder="주소 (선택)"
            placeholderTextColor={colors.placeholder}
            value={address}
            onChangeText={setAddress}
          />

          <TouchableOpacity style={styles.check} onPress={() => { setAgreeTerms((v) => !v); clearError('agree'); }}>
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
          <TouchableOpacity style={styles.check} onPress={() => { setAgreePrivacy((v) => !v); clearError('agree'); }}>
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
          <TouchableOpacity style={styles.check} onPress={() => { setAgreeSensitive((v) => !v); clearError('agree'); }}>
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
          {errors.agree && <Text style={styles.error}>{errors.agree}</Text>}

          <Button title="다음" onPress={onSubmit} loading={submitting} style={styles.nextBtn} />
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
  smallBtnText: { fontSize: font.sub },
  verifiedText: { color: colors.success, fontSize: font.sub, marginBottom: spacing.md },
  hint: { color: colors.textSub, fontSize: font.caption, marginBottom: spacing.md, marginTop: -spacing.xs },
  // 에러는 해당 칸 바로 아래에 붙는다. hint와 자리가 같아서 둘 중 하나만 보인다.
  error: { color: colors.danger, fontSize: font.caption, marginBottom: spacing.md, marginTop: -spacing.xs },
  notice: { color: colors.textSub, fontSize: font.caption, marginBottom: spacing.md, marginTop: -spacing.xs },
  inputError: { borderWidth: 1, borderColor: colors.danger },
  check: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm },
  checkText: { flex: 1, marginLeft: spacing.sm, fontSize: font.sub, color: colors.text },
  checkLink: { fontSize: font.caption, color: colors.placeholder, textDecorationLine: 'underline' },
  nextBtn: { marginTop: spacing.lg },
});