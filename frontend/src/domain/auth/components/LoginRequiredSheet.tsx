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

// [기능, 로그인 없이 가능한지]. 로그인하면 전부 가능하다
const FEATURES: [string, boolean][] = [
  ['AI 증상 상담', true],
  ['병원·약국 찾기', true],
  ['약물 정보 조회', true],
  ['상담 이력 저장', false],
  ['보호자 알림 발송', false],
  ['건강 프로필 관리', false],
];

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

          <View style={styles.table}>
            <View style={styles.tableRow}>
              <View style={styles.labelCell} />
              <View style={styles.markCell}>
                <Text style={styles.colTitle}>로그인 없이</Text>
              </View>
              <View style={[styles.markCell, styles.loginCell, styles.loginCellTop]}>
                <Text style={[styles.colTitle, styles.colTitleLogin]}>로그인</Text>
              </View>
            </View>
            {FEATURES.map(([label, guestOk], i) => (
              <View key={label} style={styles.tableRow}>
                <View style={styles.labelCell}>
                  <Text style={styles.featureText}>{label}</Text>
                </View>
                <View style={styles.markCell}>
                  {guestOk ? (
                    <Ionicons name="checkmark" size={18} color={colors.textSub} />
                  ) : (
                    <Ionicons name="close" size={18} color={colors.disabled} />
                  )}
                </View>
                <View style={[styles.markCell, styles.loginCell, i === FEATURES.length - 1 && styles.loginCellBottom]}>
                  <Ionicons name="checkmark" size={18} color={colors.primary} />
                </View>
              </View>
            ))}
          </View>

          <View style={styles.buttonRow}>
            <TouchableOpacity style={styles.guestBtn} onPress={onClose}>
              <Text style={styles.guestText}>로그인 없이 이용</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.loginBtn} onPress={goLogin}>
              <Text style={styles.loginBtnText}>로그인 / 회원가입</Text>
            </TouchableOpacity>
          </View>
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
  table: { width: '100%', marginBottom: spacing.lg },
  tableRow: { flexDirection: 'row', alignItems: 'center', minHeight: 38 },
  labelCell: { flex: 1, paddingLeft: spacing.xs },
  markCell: { width: 84, height: 38, alignItems: 'center', justifyContent: 'center' },
  loginCell: { backgroundColor: colors.primaryLight },
  loginCellTop: { borderTopLeftRadius: radius.md, borderTopRightRadius: radius.md },
  loginCellBottom: { borderBottomLeftRadius: radius.md, borderBottomRightRadius: radius.md },
  colTitle: { fontSize: font.caption, color: colors.textSub, fontWeight: '700' },
  colTitleLogin: { color: colors.primary },
  featureText: { fontSize: font.body, color: colors.text, fontWeight: '600' },
  buttonRow: { width: '100%', flexDirection: 'row', gap: spacing.sm },
  guestBtn: {
    flex: 1,
    height: 52,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  guestText: { color: colors.text, fontSize: font.body, fontWeight: '700' },
  loginBtn: {
    flex: 1.2,
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loginBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});