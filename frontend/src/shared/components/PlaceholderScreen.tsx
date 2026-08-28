import { StyleSheet, Text, View } from 'react-native';

import { colors } from '../theme/theme';

type Props = {
  name: string;
};

export default function PlaceholderScreen({ name }: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.text}>{name}</Text>
      <Text style={styles.sub}>준비 중인 화면입니다</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  text: { fontSize: 22, fontWeight: 'bold' },
  sub: { marginTop: 8, color: colors.textSub },
});