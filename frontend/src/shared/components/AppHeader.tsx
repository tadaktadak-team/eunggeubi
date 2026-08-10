import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors } from '../theme/theme';

type Props = {
  title: string;
  onBack?: () => void; // 없으면 기본 뒤로가기
};

export default function AppHeader({ title, onBack }: Props) {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation();

  const handleBack = onBack ?? (() => navigation.goBack());

  return (
    <View style={[styles.header, { paddingTop: insets.top }]}>
      <View style={styles.row}>
        <Pressable onPress={handleBack} style={styles.side} hitSlop={8}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={styles.title}>{title}</Text>
        <View style={styles.side} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    backgroundColor: colors.white,
    borderBottomWidth: 1,
    borderBottomColor: colors.border,
  },
  row: {
    height: 52,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 8,
  },
  side: {
    width: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 17,
    fontWeight: 'bold',
    color: colors.text,
  },
});
