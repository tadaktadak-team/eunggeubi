import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useRoute } from '@react-navigation/native';
import { useState } from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { showAlert } from '../../../shared/dialog';
import Button from '../../../shared/components/Button';
import { colors, font, spacing } from '../../../shared/theme/theme';
import { completeSocialSignup } from '../api/social';
import { useAuth } from '../hooks/useAuth';

type Rt = RouteProp<RootStackParamList, 'SocialConsent'>;

export default function SocialConsentScreen() {
  const { ticket } = useRoute<Rt>().params;
  const { signInWithTokens } = useAuth();

  const [agreeService, setAgreeService] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [agreeSensitive, setAgreeSensitive] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const allChecked = agreeService && agreePrivacy && agreeSensitive;

  const toggleAll = () => {
    const next = !allChecked;
    setAgreeService(next);
    setAgreePrivacy(next);
    setAgreeSensitive(next);
  };

  const onSubmit = async () => {
    if (!allChecked) {
      showAlert('약관 동의', '필수 약관에 모두 동의해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      const res = await completeSocialSignup(ticket, {
        agreeService,
        agreePrivacy,
        agreeSensitiveInfo: agreeSensitive,
      });
      await signInWithTokens(res);
      // 성공하면 AuthProvider가 로그인 상태로 바꿔 자동으로 홈으로 전환됨
    } catch (e: any) {
      showAlert('가입 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <Text style={styles.title}>약관 동의</Text>
        <Text style={styles.subtitle}>서비스 이용을 위해 아래 약관에 동의해주세요.</Text>

        <TouchableOpacity style={styles.allRow} onPress={toggleAll}>
          <Ionicons
            name={allChecked ? 'checkbox' : 'square-outline'}
            size={24}
            color={allChecked ? colors.primary : colors.placeholder}
          />
          <Text style={styles.allText}>전체 동의</Text>
        </TouchableOpacity>

        <View style={styles.divider} />

        <TouchableOpacity style={styles.row} onPress={() => setAgreeService((v) => !v)}>
          <Ionicons
            name={agreeService ? 'checkbox' : 'square-outline'}
            size={22}
            color={agreeService ? colors.primary : colors.placeholder}
          />
          <Text style={styles.rowText}>[필수] 이용약관 동의</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.row} onPress={() => setAgreePrivacy((v) => !v)}>
          <Ionicons
            name={agreePrivacy ? 'checkbox' : 'square-outline'}
            size={22}
            color={agreePrivacy ? colors.primary : colors.placeholder}
          />
          <Text style={styles.rowText}>[필수] 개인정보 처리 동의</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.row} onPress={() => setAgreeSensitive((v) => !v)}>
          <Ionicons
            name={agreeSensitive ? 'checkbox' : 'square-outline'}
            size={22}
            color={agreeSensitive ? colors.primary : colors.placeholder}
          />
          <Text style={styles.rowText}>[필수] 민감정보 처리 동의</Text>
        </TouchableOpacity>

        <Button
          title="동의하고 시작하기"
          onPress={onSubmit}
          disabled={!allChecked}
          loading={submitting}
          style={styles.btn}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  container: { flex: 1, paddingHorizontal: spacing.xl, paddingTop: spacing.xxl },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, marginBottom: spacing.sm },
  subtitle: { fontSize: font.body, color: colors.textSub, marginBottom: spacing.xl },
  allRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.md },
  allText: { marginLeft: spacing.sm, fontSize: font.body, fontWeight: '700', color: colors.text },
  divider: { height: 1, backgroundColor: colors.border, marginVertical: spacing.sm },
  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.md },
  rowText: { marginLeft: spacing.sm, fontSize: font.sub, color: colors.text },
  btn: { marginTop: spacing.xxl },
});