import { Ionicons } from '@expo/vector-icons';
import * as Clipboard from 'expo-clipboard';
import { Alert, Linking, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../../../shared/components/AppHeader';
import { SUPPORT_EMAIL } from '../../../shared/constants/contact';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';

const EMERGENCY_LINES: { label: string; number: string; desc: string }[] = [
  { label: '응급의료 상담', number: '1339', desc: '24시간 응급처치 안내' },
  { label: '자살예방 상담', number: '109', desc: '24시간 정신건강 상담' },
];

export default function SupportScreen() {
  const ready = SUPPORT_EMAIL.length > 0;

  const onCopy = async () => {
    await Clipboard.setStringAsync(SUPPORT_EMAIL);
    Alert.alert('복사됨', '문의 이메일 주소를 복사했어요.');
  };

  // 기기·버전을 미리 채워두면 되묻는 왕복을 줄일 수 있다.
  const onCompose = () => {
    const body = `\n\n\n--------------------\n앱 버전: 1.0.0\n기기: ${Platform.OS} ${Platform.Version}`;
    const url = `mailto:${SUPPORT_EMAIL}?subject=${encodeURIComponent('[응급이] 문의')}&body=${encodeURIComponent(body)}`;
    Linking.openURL(url).catch(() => Alert.alert('오류', '메일 앱을 열 수 없어요.'));
  };

  return (
    <View style={styles.container}>
      <AppHeader title="고객센터" />
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>문의하기</Text>
          <Text style={styles.cardSub}>
            오류 제보나 이용 문의를 보내주시면 확인 후 회신드려요.
          </Text>

          {ready ? (
            <>
              <Pressable style={styles.emailRow} onPress={onCopy}>
                <Text style={styles.email} numberOfLines={1}>
                  {SUPPORT_EMAIL}
                </Text>
                <Ionicons name="copy-outline" size={18} color={colors.textSub} />
              </Pressable>
              <Pressable style={styles.mailBtn} onPress={onCompose}>
                <Ionicons name="mail-outline" size={18} color={colors.white} />
                <Text style={styles.mailBtnText}>메일 앱으로 문의 작성</Text>
              </Pressable>
            </>
          ) : (
            <Text style={styles.pending}>문의 이메일은 준비 중이에요.</Text>
          )}
        </View>

        <Text style={styles.sectionTitle}>상담 전화</Text>
        <View style={styles.card}>
          {EMERGENCY_LINES.map((l, i) => (
            <Pressable
              key={l.number}
              style={[styles.lineRow, i > 0 && styles.lineBorder]}
              onPress={() => Linking.openURL(`tel:${l.number}`)}
            >
              <View style={styles.lineBadge}>
                <Text style={styles.lineBadgeText}>{l.number}</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.lineLabel}>{l.label}</Text>
                <Text style={styles.lineDesc}>{l.desc}</Text>
              </View>
              <Ionicons name="call-outline" size={18} color={colors.textSub} />
            </Pressable>
          ))}
        </View>

        <Text style={styles.notice}>
          응급이는 의료 서비스가 아니며, 앱의 안내는 참고용입니다.{'\n'}
          위급한 상황에서는 반드시 119로 연락해주세요.
        </Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.inputBg },
  content: { padding: spacing.lg, gap: spacing.md, paddingBottom: spacing.xxl },

  card: { backgroundColor: colors.white, borderRadius: radius.md, padding: spacing.lg, gap: spacing.sm },
  cardTitle: { fontSize: font.h3, fontWeight: '800', color: colors.text },
  cardSub: { fontSize: font.sub, color: colors.textSub, lineHeight: 20 },

  emailRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.inputBg,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    marginTop: spacing.xs,
  },
  email: { flex: 1, fontSize: font.body, color: colors.text, fontWeight: '600' },
  mailBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.sm,
    backgroundColor: colors.primary,
    height: 48,
    borderRadius: radius.md,
  },
  mailBtnText: { color: colors.white, fontSize: font.body, fontWeight: '700' },
  pending: { fontSize: font.sub, color: colors.placeholder, marginTop: spacing.xs },

  sectionTitle: { fontSize: font.sub, fontWeight: '700', color: colors.textSub, marginTop: spacing.sm },
  lineRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingVertical: spacing.md },
  lineBorder: { borderTopWidth: 1, borderTopColor: colors.border },
  lineBadge: {
    minWidth: 48,
    paddingHorizontal: spacing.sm,
    paddingVertical: 4,
    borderRadius: radius.sm,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
  },
  lineBadgeText: { color: colors.textSub, fontSize: font.sub, fontWeight: '800' },
  lineLabel: { fontSize: font.body, color: colors.text, fontWeight: '600' },
  lineDesc: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },

  notice: { fontSize: font.caption, color: colors.placeholder, lineHeight: 18, textAlign: 'center' },
});
