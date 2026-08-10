import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { deleteGuardian, getGuardians } from '../api/guardian';
import { Guardian, MyPageStackParamList, RELATIONSHIP_LABEL } from '../types';

type Nav = NativeStackNavigationProp<MyPageStackParamList>;

export default function GuardianScreen() {
  const navigation = useNavigation<Nav>();
  const [guardians, setGuardians] = useState<Guardian[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setGuardians(await getGuardians());
    } catch (e: any) {
      Alert.alert('오류', e?.message ?? '보호자 목록을 불러오지 못했어요.');
    } finally {
      setLoading(false);
    }
  }, []);

  // 화면에 들어올 때마다 최신 목록 (등록/수정 후 복귀 시 반영)
  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onDelete = (g: Guardian) =>
    Alert.alert('삭제', `${g.name} 보호자를 삭제할까요?`, [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제',
        style: 'destructive',
        onPress: async () => {
          try {
            await deleteGuardian(g.id);
            setGuardians((prev) => prev.filter((x) => x.id !== g.id));
          } catch (e: any) {
            Alert.alert('삭제 실패', e?.message ?? '다시 시도해주세요.');
          }
        },
      },
    ]);

  return (
    <View style={styles.container}>
      <AppHeader title="보호자 관리" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.subRow}>
          <Text style={styles.subText}>긴급상황 시 알림을 받을 연락처</Text>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{guardians.length}명</Text>
          </View>
        </View>

        {loading ? (
          <ActivityIndicator color={colors.primary} style={{ marginTop: spacing.xl }} />
        ) : (
          guardians.map((g) => (
            <View key={g.id} style={styles.card}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>{g.name.charAt(0)}</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.name}>
                  {g.name} <Text style={styles.relation}>{RELATIONSHIP_LABEL[g.relationship]}</Text>
                </Text>
                <Text style={styles.phone}>{g.phone}</Text>
              </View>
              <Pressable onPress={() => navigation.navigate('GuardianForm', { guardian: g })} hitSlop={8}>
                <Text style={styles.editBtn}>수정</Text>
              </Pressable>
              <Pressable onPress={() => onDelete(g)} hitSlop={8} style={{ marginLeft: spacing.sm }}>
                <Ionicons name="close" size={20} color={colors.placeholder} />
              </Pressable>
            </View>
          ))
        )}

        <Pressable style={styles.addBtn} onPress={() => navigation.navigate('GuardianForm')}>
          <Ionicons name="add" size={20} color={colors.textSub} />
          <Text style={styles.addText}>보호자 등록</Text>
        </Pressable>

        <View style={styles.note}>
          <Text style={styles.noteText}>
            등록된 보호자에게는 긴급 상황 발생 시 위치 및 건강정보가 자동 전송됩니다.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, gap: spacing.md },
  subRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  subText: { fontSize: font.sub, color: colors.textSub },
  badge: { backgroundColor: colors.primary, borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: 2 },
  badgeText: { color: colors.white, fontSize: font.caption, fontWeight: '700' },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    backgroundColor: colors.white,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.lg,
  },
  avatar: { width: 40, height: 40, borderRadius: 20, backgroundColor: colors.primaryLight, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.primary, fontWeight: '700', fontSize: font.body },
  name: { fontSize: font.body, fontWeight: '700', color: colors.text },
  relation: { fontSize: font.caption, fontWeight: '400', color: colors.textSub },
  phone: { fontSize: font.sub, color: colors.textSub, marginTop: 2 },
  editBtn: { fontSize: font.sub, color: colors.textSub },
  addBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingVertical: spacing.lg,
  },
  addText: { color: colors.textSub, fontSize: font.body, fontWeight: '600' },
  note: { backgroundColor: colors.primaryLight, borderRadius: radius.md, padding: spacing.md },
  noteText: { color: colors.primaryDark, fontSize: font.caption, lineHeight: 18 },
});
