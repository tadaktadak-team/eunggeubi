import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';

export default function SplashScreen() {
  return (
    <View style={styles.container}>
      <View style={styles.logo}>
        <Ionicons name="heart" size={44} color={colors.white} />
      </View>
      <Text style={styles.title}>응급이</Text>
      <Text style={styles.subtitle}>AI 기반 응급 건강 도우미{'\n'}언제 어디서나 빠르게</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center' },
  logo: {
    width: 96, height: 96, borderRadius: 28, backgroundColor: colors.primary,
    alignItems: 'center', justifyContent: 'center', marginBottom: spacing.xl,
  },
  title: { fontSize: font.h1, fontWeight: '800', color: colors.text, marginBottom: spacing.sm },
  subtitle: { fontSize: font.body, color: colors.textSub, textAlign: 'center', lineHeight: 22 },
});