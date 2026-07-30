import { useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import LoginRequiredSheet from '../components/LoginRequiredSheet';
import { useAuth } from '../hooks/useAuth';

export default function MyScreen() {
  const { isLoggedIn, userId, signOut } = useAuth();
  const [sheetVisible, setSheetVisible] = useState(false);

  const onLogout = async () => {
    try {
      await signOut();
    } catch (e: any) {
      Alert.alert('로그아웃 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <Text style={styles.title}>마이</Text>
        {isLoggedIn ? (
          <>
            <Text style={styles.info}>로그인 상태 (회원 #{userId})</Text>
            <TouchableOpacity style={styles.btn} onPress={onLogout}>
              <Text style={styles.btnText}>로그아웃</Text>
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
  btnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});