import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useCallback, useState } from 'react';
import { ActivityIndicator, Alert, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { formatPhone } from '../../../shared/utils/phone';
import { deleteGuardian, getGuardians, updateGuardian } from '../api/guardian';
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

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const onToggleNotify = async (g: Guardian) => {
    const next = !g.notifyEnabled;
    setGuardians((prev) => prev.map((x) => (x.id === g.id ? { ...x, notifyEnabled: next } : x)));
    try {
      await updateGuardian(g.id, {
        name: g.name,
        phone: g.phone,
        relationship: g.relationship,
        notifyEnabled: next,
      });
    } catch (e: any) {
      setGuardians((prev) => prev.map((x) => (x.id === g.id ? { ...x, notifyEnabled: g.notifyEnabled } : x)));
      Alert.alert('변경 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const runDelete = async (g: Guardian) => {
    try {
      await deleteGuardian(g.id);
      setGuardians((prev) => prev.filter((x) => x.id !== g.id));
    } catch (e: any) {
      Alert.alert('삭제 실패', e?.message ?? '다시 시도해주세요.');
    }
  };

  const openMenu = (g: Guardian) =>
    Alert.alert(g.name, '작업을 선택하세요', [
      { text: '수정', onPress: () => navigation.navigate('GuardianForm', { guardian: g }) },
      { text: '삭제', style: 'destructive', onPress: () => runDelete(g) },
      { text: '취소', style: 'cancel' },
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
              <View style={{ flex: 1 }}>
                <View style={styles.nameRow}>
                  <Text style={styles.name}>{g.name}</Text>
                  <View style={styles.relChip}>
                    <Text style={styles.relChipText}>{RELATIONSHIP_LABEL[g.relationship]}</Text>
                  </View>
                </View>
                <Text style={styles.phone}>{formatPhone(g.phone)}</Text>
              </View>
              <Switch
                value={g.notifyEnabled}
                onValueChange={() => onToggleNotify(g)}
                trackColor={{ true: colors.primary, false: colors.border }}
                thumbColor={colors.white}
                style={{ marginTop: 4 }}
              />
              <Pressable onPress={() => openMenu(g)} hitSlop={8} style={styles.menuBtn}>
                <Ionicons name="ellipsis-vertical" size={20} color={colors.placeholder} />
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
    gap: spacing.sm,
    backgroundColor: colors.white,
    borderRadius: radius.lg,
    borderWidth: 1,
    borderColor: colors.border,
    paddingVertical: spacing.lg,
    paddingHorizontal: spacing.lg,
  },
  name: { fontSize: font.h3, fontWeight: '700', color: colors.text },
  phone: { fontSize: font.body, color: colors.textSub, marginTop: 2 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  relChip: {
    backgroundColor: colors.primaryLight,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
  },
  relChipText: { fontSize: font.caption, color: colors.primaryDark, fontWeight: '600' },
  menuBtn: {
    width: 32,
    height: 32,
    justifyContent: 'center',
    alignItems: 'center',
  },
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