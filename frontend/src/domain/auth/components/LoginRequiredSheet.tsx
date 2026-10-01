import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Modal, Pressable, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type Nav = NativeStackNavigationProp<RootStackParamList>;

interface Props {
  visible: boolean;
  onClose: () => void;
}

const AVAILABLE = ['AI 증상 상담', '병원·약국 찾기', '약물 정보 조회'];
const LOCKED = ['상담 이력 저장', '보호자 알림 발송', '건강 프로필 관리'];

export default function LoginRequiredSheet({ visible, onClose }: Props) {
  const navigation = useNavigation<Nav>();

  const goLogin = () => {
    onClose();
    navigation.navigate('Login');
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      {/* 배경(어두운 부분) 누르면 닫힘 */}
      <Pressable style={styles.backdrop} onPress={onClose}>
        {/* 시트 본체는 눌러도 안 닫히게 */}
        <Pressable style={styles.sheet} onPress={() => {}}>
          <View style={styles.handle} />

          <View style={styles.iconCircle}>
            <Ionicons name="shield-checkmark" size={28} color={colors.primary} />
          </View>
          <Text style={styles.title}>로그인하면 이런 기능이 더해져요</Text>
          <Text style={styles.subtitle}>
            지금도 상담·병원 찾기·약물 정보는 바로 쓸 수 있어요.{'\n'}로그인하면 보호자 알림과 기록 저장까지 이어져요.
          </Text>

          <View style={styles.featureBox}>
            <View style={styles.column}>
              <Text style={styles.columnTitle}>지금 바로 이용</Text>
              {AVAILABLE.map((f) => (
                <View key={f} style={styles.featureRow}>
                  <Ionicons name="checkmark" size={18} color={colors.success} />
                  <Text style={styles.featureText}>{f}</Text>
                </View>
              ))}
            </View>
            <View style={styles.divider} />
            <View style={styles.column}>
              <Text style={styles.columnTitle}>로그인하면 추가</Text>
              {LOCKED.map((f) => (
                <View key={f} style={styles.featureRow}>
                  <Ionicons name="close" size={18} color={colors.placeholder} />
                  <Text style={[styles.featureText, styles.featureLocked]}>{f}</Text>
                </View>
              ))}
            </View>
          </View>

          <TouchableOpacity style={styles.loginBtn} onPress={goLogin}>
            <Text style={styles.loginBtnText}>로그인 / 회원가입</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.guestBtn} onPress={onClose}>
            <Text style={styles.guestText}>로그인 없이 계속 이용하기</Text>
          </TouchableOpacity>
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  sheet: {
    backgroundColor: colors.white,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: spacing.xl,
    paddingTop: spacing.md,
    paddingBottom: spacing.xxl,
    alignItems: 'center',
  },
  handle: { width: 40, height: 4, borderRadius: 2, backgroundColor: colors.border, marginBottom: spacing.lg },
  iconCircle: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#FCE9E7',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  title: { fontSize: font.h3, fontWeight: '800', color: colors.text, marginBottom: spacing.sm },
  subtitle: {
    fontSize: font.sub,
    color: colors.textSub,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: spacing.lg,
  },
  featureBox: {
    width: '100%',
    flexDirection: 'row',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.lg,
    marginBottom: spacing.lg,
  },
  column: { flex: 1 },
  columnTitle: { fontSize: font.caption, color: colors.textSub, fontWeight: '700', marginBottom: spacing.sm },
  divider: { width: 1, backgroundColor: colors.border, marginHorizontal: spacing.md },
  featureRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.xs },
  featureText: { flexShrink: 1, marginLeft: spacing.xs, fontSize: font.sub + 1, color: colors.text, fontWeight: '600' },
  featureLocked: { color: colors.placeholder, fontWeight: '400' },
  loginBtn: {
    width: '100%',
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  loginBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  guestBtn: { paddingVertical: spacing.sm },
  guestText: { color: colors.textSub, fontSize: font.sub },
});