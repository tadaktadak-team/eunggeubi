import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type Nav = NativeStackNavigationProp<RootStackParamList>;
type Rt = RouteProp<RootStackParamList, 'SignupComplete'>;

export default function SignupCompleteScreen() {
  const navigation = useNavigation<Nav>();
  const { email } = useRoute<Rt>().params;

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <View style={styles.iconCircle}>
          <Ionicons name="checkmark" size={40} color={colors.primary} />
        </View>
        <Text style={styles.title}>가입 완료!</Text>
        <Text style={styles.subtitle}>
          응급이 회원이 되신 것을 환영해요.{'\n'}지금 바로 로그인해보세요.
        </Text>

        <View style={styles.accountBox}>
          <Text style={styles.accountLabel}>가입된 계정</Text>
          <Text style={styles.accountEmail}>{email}</Text>
        </View>

        <TouchableOpacity style={styles.btn} onPress={() => navigation.navigate('Login')}>
          <Text style={styles.btnText}>시작하기</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: spacing.xl },
  iconCircle: {
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: '#FCE9E7',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.xl,
  },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, marginBottom: spacing.md },
  subtitle: { fontSize: font.body, color: colors.textSub, textAlign: 'center', lineHeight: 22, marginBottom: spacing.xxl },
  accountBox: {
    width: '100%',
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    padding: spacing.lg,
    marginBottom: spacing.xl,
  },
  accountLabel: { fontSize: font.caption, color: colors.textSub, marginBottom: spacing.xs },
  accountEmail: { fontSize: font.body, fontWeight: '700', color: colors.text },
  btn: {
    width: '100%',
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});