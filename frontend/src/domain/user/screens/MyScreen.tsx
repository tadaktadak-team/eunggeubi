import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import {
  Alert,
  Modal,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import LoginRequiredSheet from '../../auth/components/LoginRequiredSheet';
import { useAuth } from '../../auth/hooks/useAuth';

type Nav = NativeStackNavigationProp<RootStackParamList>;

export default function MyScreen() {
  const navigation = useNavigation<Nav>();
  const { isLoggedIn, userId, signOut, withdraw } = useAuth();
  const [sheetVisible, setSheetVisible] = useState(false);
  const [withdrawVisible, setWithdrawVisible] = useState(false);
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const onLogout = async () => {
    try {
      await signOut();
    } catch (e: any) {
      Alert.alert('로그아웃 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const closeWithdraw = () => {
    setWithdrawVisible(false);
    setPassword('');
  };

  const onWithdraw = async () => {
    if (!password) {
      Alert.alert('입력 확인', '비밀번호를 입력해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await withdraw(password);
      closeWithdraw();
      Alert.alert('탈퇴 완료', '그동안 이용해주셔서 감사합니다.');
    } catch (e: any) {
      Alert.alert('탈퇴 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <Text style={styles.title}>마이</Text>
        {isLoggedIn ? (
          <>
            <Text style={styles.info}>로그인 상태 (회원 #{userId})</Text>
            <TouchableOpacity style={styles.editBtn} onPress={() => navigation.navigate('EditProfile')}>
              <Text style={styles.btnText}>정보 수정</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.btn} onPress={onLogout}>
              <Text style={styles.btnText}>로그아웃</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.withdrawBtn} onPress={() => setWithdrawVisible(true)}>
              <Text style={styles.withdrawText}>회원 탈퇴</Text>
            </TouchableOpacity>
          </>
        ) : (
          <>
            <Text style={styles.info}>비회원으로 이용 중이에요</Text>
            <TouchableOpacity style={styles.btn} onPress={() => setSheetVisible(true)}>
              <Text style={styles.btnText}>로그인하고 더 이용하기</Text>
            </TouchableOpacity>
          </>
        )}
      </View>

      <LoginRequiredSheet visible={sheetVisible} onClose={() => setSheetVisible(false)} />

      <Modal
        visible={withdrawVisible}
        transparent
        animationType="fade"
        onRequestClose={closeWithdraw}
      >
        <View style={styles.backdrop}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>회원 탈퇴</Text>
            <Text style={styles.modalDesc}>
              탈퇴하면 계정이 비활성화되고 다시 로그인할 수 없어요.{'\n'}계속하려면 비밀번호를 입력해주세요.
            </Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호"
              placeholderTextColor={colors.placeholder}
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
            <TouchableOpacity
              style={[styles.confirmBtn, submitting && { opacity: 0.6 }]}
              onPress={onWithdraw}
              disabled={submitting}
            >
              <Text style={styles.confirmText}>탈퇴하기</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.cancelBtn} onPress={closeWithdraw} disabled={submitting}>
              <Text style={styles.cancelText}>취소</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: spacing.xl },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, marginBottom: spacing.xl },
  info: { fontSize: font.body, color: colors.textSub, marginBottom: spacing.xl },
  btn: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.xxl,
    height: 50,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  editBtn: {
    backgroundColor: colors.primary,
    paddingHorizontal: spacing.xxl,
    height: 50,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  btnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  withdrawBtn: { marginTop: spacing.lg },
  withdrawText: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    paddingHorizontal: spacing.xl,
  },
  modalCard: { backgroundColor: colors.white, borderRadius: radius.lg, padding: spacing.xl },
  modalTitle: { fontSize: font.h2, fontWeight: '800', color: colors.text, marginBottom: spacing.md },
  modalDesc: { fontSize: font.sub, color: colors.textSub, lineHeight: 20, marginBottom: spacing.lg },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    fontSize: font.body,
    color: colors.text,
    marginBottom: spacing.md,
  },
  confirmBtn: {
    backgroundColor: colors.danger,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  cancelBtn: { height: 44, alignItems: 'center', justifyContent: 'center', marginTop: spacing.xs },
  cancelText: { color: colors.textSub, fontSize: font.sub },
});