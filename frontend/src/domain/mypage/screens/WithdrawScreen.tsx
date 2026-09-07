import { Ionicons } from '@expo/vector-icons';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { useAuth } from '../../auth/hooks/useAuth';
import { getMyInfo, withdraw } from '../api/user';

const NOTICES = [
  '등록한 보호자와 건강 프로필이 모두 삭제돼요.',
  'AI 상담 이력을 다시 볼 수 없어요.',
  '탈퇴 후에는 같은 계정으로 다시 로그인할 수 없어요.',
];

export default function WithdrawScreen() {
  const { signOut } = useAuth();

  const [password, setPassword] = useState('');
  const [socialOnly, setSocialOnly] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const info = await getMyInfo();
      setSocialOnly(info.socialOnly);
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '내 정보를 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const runWithdraw = async () => {
    try {
      setSubmitting(true);
      await withdraw(socialOnly ? undefined : password);
      Alert.alert('탈퇴 완료', '그동안 이용해주셔서 감사합니다.', [
        { text: '확인', onPress: () => signOut() },
      ]);
    } catch (e: any) {
      Alert.alert('탈퇴 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  const onSubmit = () => {
    if (!agreed) {
      Alert.alert('확인 필요', '안내 사항에 동의해주세요.');
      return;
    }
    if (!socialOnly && !password) {
      Alert.alert('비밀번호 확인', '비밀번호를 입력해주세요.');
      return;
    }
    Alert.alert('회원 탈퇴', '정말 탈퇴하시겠어요? 되돌릴 수 없어요.', [
      { text: '취소', style: 'cancel' },
      { text: '탈퇴', style: 'destructive', onPress: runWithdraw },
    ]);
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <AppHeader title="회원 탈퇴" />
        <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xxl }} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <AppHeader title="회원 탈퇴" />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets
      >
        <View style={styles.noticeBox}>
          <Text style={styles.noticeTitle}>탈퇴하기 전에 확인해주세요</Text>
          {NOTICES.map((text) => (
            <View key={text} style={styles.noticeRow}>
              <Ionicons name="ellipse" size={5} color={colors.primaryDark} style={{ marginTop: 7 }} />
              <Text style={styles.noticeText}>{text}</Text>
            </View>
          ))}
        </View>

        {!socialOnly && (
          <>
            <Text style={styles.label}>비밀번호 확인</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="현재 비밀번호"
              placeholderTextColor={colors.placeholder}
              secureTextEntry
              autoCapitalize="none"
            />
          </>
        )}

        <Pressable style={styles.agreeRow} onPress={() => setAgreed((v) => !v)}>
          <Ionicons
            name={agreed ? 'checkbox' : 'square-outline'}
            size={22}
            color={agreed ? colors.primary : colors.placeholder}
          />
          <Text style={styles.agreeText}>안내 사항을 모두 확인했으며 탈퇴에 동의합니다</Text>
        </Pressable>

        <Pressable
          style={[styles.withdrawBtn, submitting && { opacity: 0.6 }]}
          onPress={onSubmit}
          disabled={submitting}
        >
          <Text style={styles.withdrawText}>{submitting ? '처리 중...' : '탈퇴하기'}</Text>
        </Pressable>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.md },
  noticeBox: {
    backgroundColor: colors.primaryLight,
    borderRadius: radius.md,
    padding: spacing.lg,
    gap: spacing.sm,
  },
  noticeTitle: { fontSize: font.body, fontWeight: '700', color: colors.primaryDark },
  noticeRow: { flexDirection: 'row', gap: spacing.sm, alignItems: 'flex-start' },
  noticeText: { flexShrink: 1, fontSize: font.sub, color: colors.textSub, lineHeight: 20 },
  label: { fontSize: font.sub, fontWeight: '700', color: colors.text, marginTop: spacing.sm },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 50,
    fontSize: font.body,
    color: colors.text,
  },
  agreeRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginTop: spacing.sm },
  agreeText: { flexShrink: 1, fontSize: font.sub, color: colors.text },
  withdrawBtn: {
    backgroundColor: colors.text,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  withdrawText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});
