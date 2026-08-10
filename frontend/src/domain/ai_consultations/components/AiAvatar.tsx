import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';

import { colors } from '../../../shared/theme/theme';

// AI 메시지 왼쪽에 붙는 ♥ 아바타
export default function AiAvatar() {
  return (
    <View style={styles.avatar}>
      <Ionicons name="heart" size={16} color={colors.primary} />
    </View>
  );
}

const styles = StyleSheet.create({
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#FCE8E4',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
