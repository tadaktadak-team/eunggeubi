import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { changePassword } from '../api/user';
import { MyPageStackParamList } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

const MIN_LENGTH = 8; // 서버 @Size(min = 8)과 맞춘 값

export default function ChangePasswordScreen() {
  const navigation = useNavigation<Nav>();

  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [saving, setSaving] = useState(false);

  //서버와 같은 규칙으로 검증, 프론트에서도 거름
  const validate = () => {
    if (!current || !next || !confirm) return '모든 항목을 입력해주세요.';
    if (next.length < MIN_LENGTH) return `새 비밀번호는 ${MIN_LENGTH}자 이상이어야 해요.`;
    if (next !== confirm) return '새 비밀번호가 서로 달라요.';
    if (next === current) return '현재 비밀번호와 다른 비밀번호를 입력해주세요.';
    return null;
  };

  const onSave = async () => {
    const error = validate();
    if (error) {
      Alert.alert('입력 확인', error);
      return;
    }
    try {
      setSaving(true);
      await changePassword(current, next);
      Alert.alert('변경 완료', '비밀번호가 변경됐어요.', [
        { text: '확인', onPress: () => navigation.goBack() },
      ]);
    } catch (e: any) {
      // 현재 비밀번호 불일치 등 서버 메시지를 그대로 보여준다
      Alert.alert('변경 실패', e?.message ?? '다시 시도해주세요.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={styles.container}>
      <AppHeader title="비밀번호 변경" />
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        automaticallyAdjustKeyboardInsets
      >
        <Text style={styles.label}>현재 비밀번호</Text>
        <TextInput
          style={styles.input}
          value={current}
          onChangeText={setCurrent}
          placeholder="현재 비밀번호"
          placeholderTextColor={colors.placeholder}
          secureTextEntry
          autoCapitalize="none"
        />

        <Text style={styles.label}>새 비밀번호</Text>
        <TextInput
          style={styles.input}
          value={next}
          onChangeText={setNext}
          placeholder={`${MIN_LENGTH}자 이상`}
          placeholderTextColor={colors.placeholder}
          secureTextEntry
          autoCapitalize="none"
        />

        <Text style={styles.label}>새 비밀번호 확인</Text>
        <TextInput
          style={styles.input}
          value={confirm}
          onChangeText={setConfirm}
          placeholder="새 비밀번호 다시 입력"
          placeholderTextColor={colors.placeholder}
          secureTextEntry
          autoCapitalize="none"
          onSubmitEditing={onSave}
          returnKeyType="done"
        />

        <Pressable style={[styles.saveBtn, saving && { opacity: 0.6 }]} onPress={onSave} disabled={saving}>
          <Text style={styles.saveText}>{saving ? '변경 중...' : '변경하기'}</Text>
        </Pressable>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.sm },
  label: { fontSize: font.sub, fontWeight: '700', color: colors.text, marginTop: spacing.md },
  input: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    paddingHorizontal: spacing.lg,
    height: 50,
    fontSize: font.body,
    color: colors.text,
  },
  saveBtn: {
    backgroundColor: colors.primary,
    height: 52,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.xl,
  },
  saveText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
});