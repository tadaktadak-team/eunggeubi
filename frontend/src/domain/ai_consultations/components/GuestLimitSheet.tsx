import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Linking, Modal, Pressable, StyleSheet, Text, TouchableOpacity, View } from 'react-native';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

type Nav = NativeStackNavigationProp<RootStackParamList>;

interface Props {
  visible: boolean;
  onClose: () => void; // 배경 탭/안드로이드 뒤로가기 - 시트만 닫고 채팅에 남는다(이전 답변을 다시 볼 수 있게)
  onCloseButton: () => void; // "닫기" 버튼 - 더 할 수 있는 게 없으니 홈으로 보낸다
}

// 비회원 무료 AI 상담을 다 쓴 뒤 다시 보내려 할 때 띄운다. 가입하면 지금까지 기록이 계정으로 옮겨진다.
export default function GuestLimitSheet({ visible, onClose, onCloseButton }: Props) {
  const navigation = useNavigation<Nav>();

  const goSignup = () => {
    onClose();
    navigation.navigate('Signup');
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={styles.sheet} onPress={() => {}}>
          <View style={styles.handle} />

          <View style={styles.iconCircle}>
            <Ionicons name="chatbubbles" size={28} color={colors.primary} />
          </View>
          {/* 질문 10회 소진과 IP당 발급 한도 둘 다 여기로 온다 - 특정 횟수는 안 적는다 */}
          <Text style={styles.title}>무료 상담을 모두 사용했어요</Text>
          <Text style={styles.subtitle}>
            회원가입하면 AI 상담을 계속 이용할 수 있고,{'\n'}로그인 후 지금까지의 상담 기록도 가져올 수 있어요
          </Text>

          <TouchableOpacity style={styles.emergencyBox} onPress={() => Linking.openURL('tel:119')}>
            <Ionicons name="call" size={16} color={colors.primary} />
            <Text style={styles.emergencyText}>응급 상황이라면 지금 바로 119에 전화하세요</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.signupBtn} onPress={goSignup}>
            <Text style={styles.signupBtnText}>회원가입하고 계속 상담하기</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.closeBtn} onPress={onCloseButton}>
            <Text style={styles.closeText}>닫기</Text>
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
  emergencyBox: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.lg,
  },
  emergencyText: { fontSize: font.sub, color: colors.text, fontWeight: '600' },
  signupBtn: {
    width: '100%',
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  signupBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  closeBtn: { paddingVertical: spacing.sm },
  closeText: { color: colors.textSub, fontSize: font.sub },
});
