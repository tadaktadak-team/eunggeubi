import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import DisclaimerFooter from '../components/DisclaimerFooter';
import QuickLinkCard from '../components/QuickLinkCard';
import SymptomChip from '../components/SymptomChip';
import { AiConsultationStackParamList, QUICK_SYMPTOMS } from '../types';

type Nav = NativeStackNavigationProp<AiConsultationStackParamList>;

export default function SymptomHomeScreen() {
  const navigation = useNavigation<Nav>();
  const insets = useSafeAreaInsets();
  const [text, setText] = useState('');

  const goAsk = (message: string) => {
    const trimmed = message.trim();
    if (!trimmed) {
      Alert.alert('증상을 입력해주세요', '예) 어젯밤부터 두통이 심해요');
      return;
    }
    navigation.navigate('SymptomChat', { initialMessage: trimmed });
  };

  // 다음 단계(병원찾기·약물정보 탭)에서 실제 목적지로 연결하면 됩니다.
  const goToTab = (tabName: string) => {
    (navigation.getParent() as any)?.navigate(tabName);
  };

  const notReady = () => Alert.alert('준비 중', '다음 단계에서 만들 거예요!');

  return (
      <View style={styles.container}>
        <View style={[styles.header, { paddingTop: insets.top + spacing.md }]}>
          <Text style={styles.title}>{'어디가\n불편하세요?'}</Text>
          <Pressable style={styles.bellBtn} onPress={notReady} hitSlop={8}>
            <Ionicons name="notifications-outline" size={22} color={colors.text} />
          </Pressable>
        </View>

        <ScrollView contentContainerStyle={styles.scrollContent} keyboardShouldPersistTaps="handled">
          <Text style={styles.subtitle}>증상을 말하면 AI가 참고정보를 안내해요</Text>

          <View style={styles.inputCard}>
            <TextInput
                style={styles.input}
                placeholder="예) “어젯밤부터 두통이 심해요”"
                placeholderTextColor={colors.placeholder}
                value={text}
                onChangeText={setText}
                multiline
            />
            <View style={styles.inputCardFooter}>
              <Pressable style={styles.askBtn} onPress={() => goAsk(text)}>
                <Text style={styles.askBtnText}>물어보기</Text>
              </Pressable>
            </View>
          </View>

          <View style={styles.chipRow}>
            {QUICK_SYMPTOMS.map((symptom) => (
                <SymptomChip key={symptom} label={symptom} onPress={() => goAsk(symptom)} />
            ))}
          </View>

          <Text style={styles.sectionTitle}>바로가기</Text>
          <View style={styles.quickLinkRow}>
            <QuickLinkCard label="병원찾기" icon={{ lib: 'ion', name: 'location-outline' }} onPress={() => goToTab('Hospital')} />
            <QuickLinkCard
                label="응급처치 안내"
                icon={{ lib: 'ion', name: 'alert-circle-outline' }}
                onPress={() => navigation.navigate('FirstAidGuide', {})}
            />
            <QuickLinkCard label="약물정보" icon={{ lib: 'mci', name: 'pill' }} onPress={() => goToTab('Medicine')} />
          </View>

          <Text style={styles.sectionTitle}>가까운 응급실</Text>
          <Pressable style={styles.erCard} onPress={() => goToTab('Hospital')}>
            <View style={styles.erInfo}>
              <Text style={styles.erName}>OO대학병원</Text>
              <Text style={styles.erMeta}>2.1km · 혼잡도 낮음</Text>
            </View>
            <View style={styles.erBadge}>
              <Text style={styles.erBadgeText}>병상 12</Text>
            </View>
          </Pressable>
        </ScrollView>

        <DisclaimerFooter />
      </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.bg },
  header: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.xl,
    paddingBottom: spacing.md,
  },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, lineHeight: 34 },
  bellBtn: {
    width: 40,
    height: 40,
    borderRadius: radius.pill,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollContent: { paddingHorizontal: spacing.xl, paddingBottom: spacing.lg },
  subtitle: { fontSize: font.sub, color: colors.textSub, marginBottom: spacing.lg },
  inputCard: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
    marginBottom: spacing.lg,
  },
  input: { fontSize: font.body, color: colors.text, minHeight: 44, textAlignVertical: 'top' },
  inputCardFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    marginTop: spacing.md,
  },
  askBtn: {
    backgroundColor: colors.black,
    paddingHorizontal: spacing.xl,
    paddingVertical: spacing.sm + 2,
    borderRadius: radius.md,
  },
  askBtnText: { color: colors.white, fontSize: font.sub, fontWeight: '700' },
  chipRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.xl },
  sectionTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text, marginBottom: spacing.md },
  quickLinkRow: { flexDirection: 'row', gap: spacing.sm, marginBottom: spacing.xl },
  erCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.inputBg,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
  erInfo: { gap: spacing.xs },
  erName: { fontSize: font.body, fontWeight: '700', color: colors.text },
  erMeta: { fontSize: font.caption, color: colors.textSub },
  erBadge: {
    borderWidth: 1,
    borderColor: colors.primary,
    borderRadius: radius.pill,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.xs,
  },
  erBadgeText: { color: colors.primary, fontSize: font.caption, fontWeight: '700' },
});