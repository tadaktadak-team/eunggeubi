import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { useAuth } from '../hooks/useAuth';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export default function LoginScreen() {
  const navigation = useNavigation<Nav>();
  const { signIn } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const onLogin = async () => {
    if (!email || !password) {
      Alert.alert('입력 확인', '이메일과 비밀번호를 입력해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await signIn(email.trim(), password);
      // 성공 시 AuthProvider가 상태를 바꿔 자동으로 홈으로 전환됨
    } catch (e: any) {
      Alert.alert('로그인 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const notReady = () => Alert.alert('준비 중', '다음 단계에서 만들 거예요!');

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.container}>
          <View style={styles.logo}>
            <Ionicons name="heart" size={30} color={colors.white} />
          </View>
          <Text style={styles.title}>로그인</Text>

          <View style={styles.inputWrap}>
            <TextInput
              style={styles.input}
              placeholder="이메일"
              placeholderTextColor={colors.placeholder}
              autoCapitalize="none"
              keyboardType="email-address"
              value={email}
              onChangeText={setEmail}
            />
            <Ionicons name="person-outline" size={20} color={colors.placeholder} />
          </View>

          <View style={styles.inputWrap}>
            <TextInput
              style={styles.input}
              placeholder="비밀번호"
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

          <TouchableOpacity
            style={[styles.loginBtn, submitting && { opacity: 0.6 }]}
            onPress={onLogin}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.loginBtnText}>로그인</Text>
            )}
          </TouchableOpacity>

          <View style={styles.links}>
            <TouchableOpacity onPress={() => navigation.navigate('Signup')}>
              <Text style={styles.link}>회원가입</Text>
            </TouchableOpacity>
            <Text style={styles.dot}>·</Text>
            <TouchableOpacity onPress={() => navigation.navigate('FindAccount', { tab: 'FIND_ID' })}>
              <Text style={styles.link}>아이디 찾기</Text>
            </TouchableOpacity>
            <Text style={styles.dot}>·</Text>
            <TouchableOpacity onPress={() => navigation.navigate('FindAccount', { tab: 'FIND_PW' })}>
              <Text style={styles.link}>비밀번호 재설정</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={[styles.socialBtn, { backgroundColor: colors.kakao }]}
            onPress={notReady}
          >
            <Text style={[styles.socialText, { color: colors.kakaoText }]}>카카오로 시작하기</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.socialBtn, { backgroundColor: colors.naver }]}
            onPress={notReady}
          >
            <Text style={[styles.socialText, { color: colors.white }]}>네이버로 시작하기</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.guest} onPress={() => navigation.navigate('Tabs')}>
            <Text style={styles.guestText}>로그인 없이 이용하기</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.legal} onPress={() => navigation.navigate('Legal')}>
            <Text style={styles.legalText}>이용약관 · 개인정보처리방침</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  flex: { flex: 1 },
  container: { flex: 1, paddingHorizontal: spacing.xl, justifyContent: 'center' },
  logo: {
    width: 56,
    height: 56,
    borderRadius: 16,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'center',
    marginBottom: spacing.md,
  },
  title: {
    fontSize: font.h2,
    fontWeight: '800',
    color: colors.text,
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    marginBottom: spacing.md,
    height: 52,
  },
  input: { flex: 1, fontSize: font.body, color: colors.text },
  loginBtn: {
    backgroundColor: colors.black,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.sm,
  },
  loginBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  links: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginVertical: spacing.lg,
  },
  link: { color: colors.text, fontSize: font.sub, fontWeight: '600' },
  dot: { color: colors.placeholder, marginHorizontal: spacing.sm },
  socialBtn: {
    height: 50,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  socialText: { fontSize: font.body, fontWeight: '700' },
  guest: { alignItems: 'center', marginTop: spacing.md },
  guestText: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
  legal: { alignItems: 'center', marginTop: spacing.lg },
  legalText: { color: colors.placeholder, fontSize: font.caption, textDecorationLine: 'underline' },
});