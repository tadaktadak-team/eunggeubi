import { RouteProp, useRoute } from '@react-navigation/native';
import { useMemo, useRef, useState } from 'react';
import { LayoutChangeEvent, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

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

// "제N조 " 앞에서 끊어 조항 단위 블록으로 나눈다. 첫 블록은 조항 앞의 머리말일 수 있다.
const splitArticles = (doc: string) => doc.split(/\n(?=제\d+조 )/);

export default function LegalScreen() {
  const { params } = useRoute<LegalRoute>();
  const [tab, setTab] = useState<Tab>(params?.tab ?? 'terms');

  const scrollRef = useRef<ScrollView>(null);
  const scrolled = useRef(false);

  const articles = useMemo(
    () => splitArticles(tab === 'terms' ? TERMS_OF_SERVICE : PRIVACY_POLICY),
    [tab],
  );
  const target = params?.section;
  const targetIndex = target ? articles.findIndex((a) => a.startsWith(`${target} `)) : -1;

  // 조항 위치는 레이아웃이 끝나야 알 수 있어서, 목표 조항이 그려지는 순간에 스크롤한다.
  const onArticleLayout = (index: number) => (e: LayoutChangeEvent) => {
    if (index !== targetIndex || scrolled.current) return;
    scrolled.current = true;
    scrollRef.current?.scrollTo({ y: e.nativeEvent.layout.y, animated: true });
  };

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

      <ScrollView ref={scrollRef} contentContainerStyle={styles.content}>
        <Text style={styles.effective}>시행일: {EFFECTIVE_DATE}</Text>
        {articles.map((article, i) => (
          <View
            key={i}
            style={i === targetIndex && styles.highlight}
            onLayout={onArticleLayout(i)}
          >
            <Text style={styles.body}>{article}</Text>
          </View>
        ))}
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
  highlight: {
    backgroundColor: colors.primaryLight,
    borderRadius: radius.sm,
    marginHorizontal: -spacing.sm,
    paddingHorizontal: spacing.sm,
    paddingVertical: spacing.sm,
  },
});
