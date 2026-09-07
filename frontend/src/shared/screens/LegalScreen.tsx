import { RouteProp, useRoute } from '@react-navigation/native';
import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import AppHeader from '../components/AppHeader';
import { EFFECTIVE_DATE, PRIVACY_POLICY, TERMS_OF_SERVICE } from '../constants/legalDocs';
import { colors, font, radius, spacing } from '../theme/theme';
import { RootStackParamList } from '../../navigation/types';

type LegalRoute = RouteProp<RootStackParamList, 'Legal'>;
type Tab = 'terms' | 'privacy';

const TABS: { key: Tab; label: string }[] = [
  { key: 'terms', label: '이용약관' },
  { key: 'privacy', label: '개인정보처리방침' },
];

export default function LegalScreen() {
  const { params } = useRoute<LegalRoute>();
  const [tab, setTab] = useState<Tab>(params?.tab ?? 'terms');

  return (
    <View style={styles.container}>
      <AppHeader title="약관 및 정책" />

      <View style={styles.tabRow}>
        {TABS.map((t) => {
          const active = t.key === tab;
          return (
            <Pressable
              key={t.key}
              style={[styles.tab, active && styles.tabActive]}
              onPress={() => setTab(t.key)}
            >
              <Text style={[styles.tabText, active && styles.tabTextActive]}>{t.label}</Text>
            </Pressable>
          );
        })}
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.effective}>시행일: {EFFECTIVE_DATE}</Text>
        <Text style={styles.body}>{tab === 'terms' ? TERMS_OF_SERVICE : PRIVACY_POLICY}</Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  tabRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.white,
    alignItems: 'center',
  },
  tabActive: { backgroundColor: colors.primary, borderColor: colors.primary },
  tabText: { fontSize: font.sub, color: colors.textSub, fontWeight: '600' },
  tabTextActive: { color: colors.white },
  content: { padding: spacing.lg, paddingBottom: spacing.xxl, gap: spacing.md },
  effective: { fontSize: font.caption, color: colors.placeholder },
  body: { fontSize: font.sub, color: colors.text, lineHeight: 22 },
});
