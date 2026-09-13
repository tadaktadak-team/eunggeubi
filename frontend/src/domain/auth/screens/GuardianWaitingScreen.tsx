import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { ActivityIndicator, Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import * as authApi from '../api/auth';

type Nav = NativeStackNavigationProp<RootStackParamList>;
type WaitingRoute = RouteProp<RootStackParamList, 'GuardianWaiting'>;

export default function GuardianWaitingScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<WaitingRoute>();
  const { userId, email, maskedPhone } = route.params;

  const [checking, setChecking] = useState(false);

  const onCheck = async () => {
    setChecking(true);
    try {
      const res = await authApi.getGuardianConsentStatus(userId);
      if (res.confirmed) {
        navigation.navigate('SignupComplete', { email });
      } else {
        Alert.alert('아직 동의 전이에요', '보호자가 문자 링크로 동의를 완료하면 가입이 마무리돼요.');
      }
    } catch (e: any) {
      Alert.alert('확인 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setChecking(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={10}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>보호자 동의</Text>
        <View style={{ width: 26 }} />
      </View>

      <View style={styles.body}>
        <View style={styles.iconWrap}>
          <Ionicons name="call" size={30} color={colors.success} />
        </View>
        <Text style={styles.title}>보호자에게 문자를 발송했어요</Text>
        <Text style={styles.phone}>{maskedPhone}</Text>
        <Text style={styles.desc}>
          로 동의 링크가 발송되었습니다.{'\n'}보호자가 링크를 눌러 동의하면 가입이 완료돼요.
        </Text>

        <TouchableOpacity
          style={[styles.checkBtn, checking && { opacity: 0.6 }]}
          onPress={onCheck}
          disabled={checking}
        >
          {checking ? (
            <ActivityIndicator color={colors.white} />
          ) : (
            <Text style={styles.checkBtnText}>동의 완료 확인</Text>
          )}
        </TouchableOpacity>

        <TouchableOpacity style={styles.later} onPress={() => navigation.navigate('Login')}>
          <Text style={styles.laterText}>나중에 하기 (로그인 화면으로)</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    height: 52,
  },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  body: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: spacing.xl },
  iconWrap: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.lg,
  },
  title: { fontSize: font.h3, fontWeight: '800', color: colors.text, marginBottom: spacing.sm },
  phone: { fontSize: font.body, fontWeight: '700', color: colors.primary },
  desc: {
    fontSize: font.sub,
    color: colors.textSub,
    textAlign: 'center',
    lineHeight: 20,
    marginTop: spacing.xs,
    marginBottom: spacing.xl,
  },
  checkBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    alignSelf: 'stretch',
  },
  checkBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  later: { marginTop: spacing.lg },
  laterText: { color: colors.textSub, fontSize: font.sub, textDecorationLine: 'underline' },
});