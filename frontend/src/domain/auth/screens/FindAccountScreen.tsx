import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
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
import { Purpose } from '../types';

type Nav = NativeStackNavigationProp<RootStackParamList>;
type Rt = RouteProp<RootStackParamList, 'FindAccount'>;
type Tab = 'FIND_ID' | 'FIND_PW';

export default function FindAccountScreen() {
  const navigation = useNavigation<Nav>();
  const params = useRoute<Rt>().params;
  const [tab, setTab] = useState<Tab>(params?.tab ?? 'FIND_ID');

  // 공통 휴대폰 인증 상태
  const [phone, setPhone] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [code, setCode] = useState('');
  const [phoneVerified, setPhoneVerified] = useState(false);

  const [name, setName] = useState('');
  const [foundEmail, setFoundEmail] = useState<string | null>(null);

  const [email, setEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirm, setNewPasswordConfirm] = useState('');

  const [submitting, setSubmitting] = useState(false);

  const switchTab = (t: Tab) => {
    setTab(t);
    setPhone('');
    setCodeSent(false);
    setCode('');
    setPhoneVerified(false);
    setFoundEmail(null);
    setName('');
    setEmail('');
    setNewPassword('');
    setNewPasswordConfirm('');
  };

  const purpose: Purpose = tab === 'FIND_ID' ? 'FIND_ID' : 'FIND_PW';

  const sendCode = async () => {
    if (!phone) return showAlert('전화번호', '전화번호를 입력해주세요.');
    try {
      await authApi.sendPhoneCode(phone.trim(), purpose);
      setCodeSent(true);
      showAlert('인증번호 발송', '인증번호를 발송했어요.\n(개발 중: 서버 콘솔에서 확인)');
    } catch (e: any) {
      showAlert('발송 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const verifyCode = async () => {
    if (!code) return showAlert('인증번호 발송', '인증번호를 발송했어요. 문자를 확인해주세요.');
    try {
      await authApi.verifyPhoneCode(phone.trim(), purpose, code.trim());
      setPhoneVerified(true);
      showAlert('인증 완료', '휴대폰 인증이 완료됐어요.');
    } catch (e: any) {
      showAlert('인증 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const onFindEmail = async () => {
    if (!name) return showAlert('이름', '이름을 입력해주세요.');
    if (!phoneVerified) return showAlert('휴대폰 인증', '휴대폰 인증을 완료해주세요.');
    setSubmitting(true);
    try {
      const res = await authApi.findEmail(name.trim(), phone.trim());
      setFoundEmail(res.email);
    } catch (e: any) {
      showAlert('아이디 찾기 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const onResetPassword = async () => {
    if (!email || !newPassword) return showAlert('입력 확인', '이메일과 새 비밀번호를 입력해주세요.');
    const pwCheck = validatePassword(newPassword, { email: email.trim(), phone: phone.trim() });
    if (!pwCheck.ok) return showAlert('비밀번호 확인', pwCheck.message);
    if (newPassword !== newPasswordConfirm) {
      return showAlert('비밀번호 확인', '비밀번호가 일치하지 않습니다.');
    }
    if (!phoneVerified) return showAlert('휴대폰 인증', '휴대폰 인증을 완료해주세요.');
    setSubmitting(true);
    try {
      await authApi.resetPassword(email.trim(), phone.trim(), newPassword);
      showAlert('완료', '비밀번호가 변경됐어요. 새 비밀번호로 로그인해주세요.', [
        { text: '로그인하러 가기', onPress: () => navigation.navigate('Login') },
      ]);
    } catch (e: any) {
      showAlert('비밀번호 재설정 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const renderPhoneVerify = () => (
    <>
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
        <Button
          title={phoneVerified ? '완료' : '인증'}
          variant={phoneVerified ? 'success' : 'neutral'}
          onPress={sendCode}
          done={phoneVerified}
          textStyle={styles.smallBtnText}
        />
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
          <Button title="확인" variant="neutral" onPress={verifyCode} textStyle={styles.smallBtnText} />
        </View>
      )}
      {phoneVerified && <Text style={styles.verifiedText}>✓ 휴대폰 인증 완료</Text>}
    </>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={10}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>계정 찾기</Text>
        <View style={{ width: 26 }} />
      </View>

      <View style={styles.tabs}>
        <TouchableOpacity style={styles.tab} onPress={() => switchTab('FIND_ID')}>
          <Text style={[styles.tabText, tab === 'FIND_ID' && styles.tabTextActive]}>아이디 찾기</Text>
          {tab === 'FIND_ID' && <View style={styles.tabUnderline} />}
        </TouchableOpacity>
        <TouchableOpacity style={styles.tab} onPress={() => switchTab('FIND_PW')}>
          <Text style={[styles.tabText, tab === 'FIND_PW' && styles.tabTextActive]}>비밀번호 재설정</Text>
          {tab === 'FIND_PW' && <View style={styles.tabUnderline} />}
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          {tab === 'FIND_ID' ? (
            <>
              <Text style={styles.desc}>가입 시 등록한 정보로 확인합니다</Text>
              <TextInput
                style={styles.input}
                placeholder="이름"
                placeholderTextColor={colors.placeholder}
                value={name}
                onChangeText={setName}
              />
              {renderPhoneVerify()}
              <Button
                title="이메일 확인하기"
                onPress={onFindEmail}
                loading={submitting}
                style={styles.submitBtn}
              />
              {foundEmail && (
                <View style={styles.resultBox}>
                  <Text style={styles.resultLabel}>결과</Text>
                  <Text style={styles.resultEmail}>{foundEmail}</Text>
                  <Text style={styles.resultSub}>해당 이메일로 가입되어 있습니다</Text>
                  <Button
                    title="로그인하러 가기"
                    variant="secondary"
                    size="md"
                    onPress={() => navigation.navigate('Login')}
                  />
                </View>
              )}
            </>
          ) : (
            <>
              <Text style={styles.desc}>가입한 이메일과 휴대폰 인증으로 재설정합니다</Text>
              <TextInput
                style={styles.input}
                placeholder="이메일"
                placeholderTextColor={colors.placeholder}
                autoCapitalize="none"
                keyboardType="email-address"
                value={email}
                onChangeText={setEmail}
              />
              {renderPhoneVerify()}
              <TextInput
                style={styles.input}
                placeholder="새 비밀번호"
                placeholderTextColor={colors.placeholder}
                secureTextEntry
                value={newPassword}
                onChangeText={setNewPassword}
              />
              <Text style={styles.hint}>{PASSWORD_RULE_TEXT}</Text>
              <TextInput
                style={styles.input}
                placeholder="새 비밀번호 확인"
                placeholderTextColor={colors.placeholder}
                secureTextEntry
                value={newPasswordConfirm}
                onChangeText={setNewPasswordConfirm}
              />
              <Button
                title="비밀번호 변경"
                onPress={onResetPassword}
                loading={submitting}
                style={styles.submitBtn}
              />
            </>
          )}
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
  tabs: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: colors.border },
  tab: { flex: 1, alignItems: 'center', paddingVertical: spacing.md },
  tabText: { fontSize: font.body, color: colors.textSub, fontWeight: '600' },
  tabTextActive: { color: colors.primary },
  tabUnderline: { position: 'absolute', bottom: -1, height: 2, width: '60%', backgroundColor: colors.primary },
  body: { padding: spacing.xl },
  hint: { color: colors.textSub, fontSize: font.caption, marginBottom: spacing.md, marginTop: -spacing.xs },
  desc: { fontSize: font.sub, color: colors.textSub, marginBottom: spacing.lg },
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
  smallBtnText: { fontSize: font.sub },
  verifiedText: { color: colors.success, fontSize: font.sub, marginBottom: spacing.md },
  submitBtn: { marginTop: spacing.sm },
  resultBox: {
    marginTop: spacing.xl,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    padding: spacing.lg,
  },
  resultLabel: { fontSize: font.caption, color: colors.textSub, marginBottom: spacing.xs },
  resultEmail: { fontSize: font.h3, fontWeight: '800', color: colors.text },
  resultSub: { fontSize: font.sub, color: colors.textSub, marginTop: spacing.xs, marginBottom: spacing.md },
});