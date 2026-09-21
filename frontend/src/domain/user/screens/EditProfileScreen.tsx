import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useEffect, useState } from 'react';
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
import * as userApi from '../api/user';
import { Gender } from '../../auth/types';

type Nav = NativeStackNavigationProp<RootStackParamList>;

const GENDERS: { label: string; value: Gender }[] = [
  { label: '남성', value: 'MALE' },
  { label: '여성', value: 'FEMALE' },
  { label: '선택 안 함', value: 'NONE' },
];

export default function EditProfileScreen() {
  const navigation = useNavigation<Nav>();

  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [birth, setBirth] = useState(''); // 8자리로 표시
  const [gender, setGender] = useState<Gender>('NONE');
  const [address, setAddress] = useState('');

  // 화면 진입 시 현재 정보 불러오기
  useEffect(() => {
    (async () => {
      try {
        const p = await userApi.getProfile();
        setEmail(p.email);
        setName(p.name);
        setPhone(p.phone);
        setBirth(p.birthDate ? p.birthDate.replace(/-/g, '') : '');
        setGender(p.gender);
        setAddress(p.address ?? '');
      } catch (e: any) {
        Alert.alert('불러오기 실패', e?.message ?? '다시 시도해주세요.');
        navigation.goBack();
      } finally {
        setLoading(false);
      }
    })();
  }, [navigation]);

  const toISODate = (d: string) =>
    /^\d{8}$/.test(d) ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : null;

  const onSave = async () => {
    if (!name || !phone || !birth) {
      return Alert.alert('입력 확인', '이름·전화번호·생년월일은 필수예요.');
    }
    const birthDate = toISODate(birth);
    if (!birthDate) return Alert.alert('생년월일', '생년월일 8자리를 정확히 입력해주세요. (예: 19900101)');

    setSubmitting(true);
    try {
      await userApi.updateProfile({
        name: name.trim(),
        phone: phone.trim(),
        birthDate,
        gender,
        address: address.trim() || undefined,
      });
      Alert.alert('저장 완료', '회원 정보가 수정됐어요.');
      navigation.goBack();
    } catch (e: any) {
      Alert.alert('저장 실패', e?.message ?? '다시 시도해주세요.');
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
        <Text style={styles.headerTitle}>회원 정보 수정</Text>
        <View style={{ width: 26 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={colors.primary} />
        </View>
      ) : (
        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
            <Text style={styles.label}>이메일 (변경 불가)</Text>
            <TextInput style={[styles.input, styles.inputDisabled]} value={email} editable={false} />

            <Text style={styles.label}>이름</Text>
            <TextInput
              style={styles.input}
              placeholder="이름"
              placeholderTextColor={colors.placeholder}
              value={name}
              onChangeText={setName}
            />

            <Text style={styles.label}>전화번호</Text>
            <TextInput
              style={styles.input}
              placeholder="전화번호 ('-' 없이)"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              value={phone}
              onChangeText={setPhone}
            />

            <Text style={styles.label}>생년월일</Text>
            <TextInput
              style={styles.input}
              placeholder="예: 19900101"
              placeholderTextColor={colors.placeholder}
              keyboardType="number-pad"
              maxLength={8}
              value={birth}
              onChangeText={setBirth}
            />

            <Text style={styles.label}>성별</Text>
            <View style={styles.genderRow}>
              {GENDERS.map((g) => (
                <TouchableOpacity
                  key={g.value}
                  style={[styles.genderBtn, gender === g.value && styles.genderBtnActive]}
                  onPress={() => setGender(g.value)}
                >
                  <Text style={[styles.genderText, gender === g.value && styles.genderTextActive]}>
                    {g.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.label}>주소 (선택)</Text>
            <TextInput
              style={styles.input}
              placeholder="주소"
              placeholderTextColor={colors.placeholder}
              value={address}
              onChangeText={setAddress}
            />

            <TouchableOpacity
              style={[styles.saveBtn, submitting && { opacity: 0.6 }]}
              onPress={onSave}
              disabled={submitting}
            >
              {submitting ? (
                <ActivityIndicator color={colors.white} />
              ) : (
                <Text style={styles.saveBtnText}>저장하기</Text>
              )}
            </TouchableOpacity>
          </ScrollView>
        </KeyboardAvoidingView>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.white },
  flex: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.lg,
    height: 52,
  },
  headerTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  body: { padding: spacing.xl, paddingBottom: spacing.xxl },
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
  inputDisabled: { opacity: 0.6 },
  genderRow: { flexDirection: 'row', marginBottom: spacing.md },
  genderBtn: {
    flex: 1,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: spacing.sm,
  },
  genderBtnActive: { backgroundColor: colors.primary },
  genderText: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  genderTextActive: { color: colors.white },
  saveBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  saveBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});