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

// 로그인해야 쓸 수 있는 기능. 상담·병원 찾기·약물 정보는 로그인 없이도 되므로 문구로만 안내한다
const BENEFITS = ['긴급 상황에 보호자에게 위치 알림', 'AI 상담 이력 저장', '건강 프로필 관리'];

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

          <Text style={styles.title}>로그인하면 이런 기능이 더해져요</Text>
          <Text style={styles.subtitle}>상담·병원 찾기·약물 정보는 로그인 없이도 쓸 수 있어요.</Text>

          <View style={styles.list}>
            {BENEFITS.map((b) => (
              <View key={b} style={styles.row}>
                <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                <Text style={styles.rowText}>{b}</Text>
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
  },
  handle: {
    alignSelf: 'center',
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.border,
    marginBottom: spacing.xl,
  },
  title: { fontSize: font.h3, fontWeight: '800', color: colors.text },
  subtitle: { fontSize: font.sub, color: colors.textSub, marginTop: spacing.sm },
  list: { gap: spacing.md, marginTop: spacing.xl, marginBottom: spacing.xl },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  rowText: { fontSize: font.body, color: colors.text },
  buttonRow: { flexDirection: 'row', gap: spacing.sm },
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
    height: 52,
    borderRadius: radius.md,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  loginBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});
