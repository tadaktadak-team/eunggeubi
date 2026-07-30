import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { RootStackParamList } from '../../../navigation/types';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import * as authApi from '../api/auth';
import { Relationship } from '../types';

type Nav = NativeStackNavigationProp<RootStackParamList>;
type ConsentRoute = RouteProp<RootStackParamList, 'GuardianConsent'>;

const RELATIONSHIPS: { label: string; value: Relationship }[] = [
  { label: '부모', value: 'PARENT' },
  { label: '조부모', value: 'GRANDPARENT' },
  { label: '형제·자매', value: 'SIBLING' },
  { label: '기타', value: 'OTHER' },
];

export default function GuardianConsentScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<ConsentRoute>();
  const { userId, email } = route.params;

  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [relationship, setRelationship] = useState<Relationship>('PARENT');
  const [agree, setAgree] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const onSend = async () => {
    if (!name || !phone) {
      return Alert.alert('입력 확인', '보호자 이름과 연락처를 입력해주세요.');
    }
    if (!agree) {
      return Alert.alert('동의 필요', '보호자 개인정보 수집·이용에 동의해주세요.');
    }
    setSubmitting(true);
    try {
      const res = await authApi.requestGuardianConsent({
        userId,
        name: name.trim(),
        phone: phone.trim(),
        relationship,
      });
      navigation.navigate('GuardianWaiting', { userId, email, maskedPhone: res.maskedPhone });
    } catch (e: any) {
      Alert.alert('발송 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={10}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>보호자 동의</Text>
        <View style={{ width: 26 }} />
      </View>

      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
          <View style={styles.notice}>
            <Text style={styles.noticeTitle}>만 14세 미만 가입 안내</Text>
            <Text style={styles.noticeText}>
              입력하신 생년월일 기준으로 법정대리인의 동의가 필요합니다. 보호자에게 동의 문자를 발송할게요.
            </Text>
          </View>

          <Text style={styles.label}>보호자 이름</Text>
          <TextInput
            style={styles.input}
            placeholder="보호자 이름"
            placeholderTextColor={colors.placeholder}
            value={name}
            onChangeText={setName}
          />

          <Text style={styles.label}>보호자 전화번호</Text>
          <TextInput
            style={styles.input}
            placeholder="'-' 없이 입력"
            placeholderTextColor={colors.placeholder}
            keyboardType="number-pad"
            value={phone}
            onChangeText={setPhone}
          />

          <Text style={styles.label}>보호자와의 관계</Text>
          <View style={styles.relRow}>
            {RELATIONSHIPS.map((r) => (
              <TouchableOpacity
                key={r.value}
                style={[styles.relBtn, relationship === r.value && styles.relBtnActive]}
                onPress={() => setRelationship(r.value)}
              >
                <Text style={[styles.relText, relationship === r.value && styles.relTextActive]}>
                  {r.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity style={styles.check} onPress={() => setAgree((v) => !v)}>
            <Ionicons
              name={agree ? 'checkbox' : 'square-outline'}
              size={22}
              color={agree ? colors.primary : colors.placeholder}
            />
            <Text style={styles.checkText}>[필수] 보호자 개인정보 수집·이용 동의</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.sendBtn, submitting && { opacity: 0.6 }]}
            onPress={onSend}
            disabled={submitting}
          >
            {submitting ? (
              <ActivityIndicator color={colors.white} />
            ) : (
              <Text style={styles.sendBtnText}>동의 문자 발송</Text>
            )}
          </TouchableOpacity>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    height: 52,
  },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  body: { padding: spacing.xl, paddingBottom: spacing.xxl },
  notice: { backgroundColor: colors.inputBg, borderRadius: radius.md, padding: spacing.lg, marginBottom: spacing.lg },
  noticeTitle: { fontSize: font.body, fontWeight: '700', color: colors.text, marginBottom: spacing.xs },
  noticeText: { fontSize: font.sub, color: colors.textSub, lineHeight: 20 },
  label: { fontSize: font.sub, color: colors.textSub, marginBottom: spacing.xs },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 52,
    fontSize: font.body,
    color: colors.text,
    marginBottom: spacing.md,
  },
  relRow: { flexDirection: 'row', marginBottom: spacing.md },
  relBtn: {
    flex: 1,
    height: 44,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  relBtnActive: { backgroundColor: colors.primary },
  relText: { fontSize: font.caption, color: colors.textSub, fontWeight: '600' },
  relTextActive: { color: colors.white },
  check: { flexDirection: 'row', alignItems: 'center', paddingVertical: spacing.sm, marginBottom: spacing.md },
  checkText: { marginLeft: spacing.sm, fontSize: font.sub, color: colors.text },
  sendBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.sm,
  },
  sendBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});