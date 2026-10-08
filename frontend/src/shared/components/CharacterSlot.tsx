import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, View } from 'react-native';

import { colors } from '../theme/theme';

type Props = {
  size?: number;
};

// 안내 문구 위에 들어갈 캐릭터 자리. 캐릭터 이미지가 나오면 이 안만 Image로 바꾸면 된다
export default function CharacterSlot({ size = 120 }: Props) {
  return (
    <View style={[styles.slot, { width: size, height: size, borderRadius: size / 2 }]}>
      <Ionicons name="happy-outline" size={size * 0.4} color={colors.placeholder} />
    </View>
  );
}

const styles = StyleSheet.create({
  slot: {
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderStyle: 'dashed',
    borderColor: colors.border,
    backgroundColor: 'rgba(255,255,255,0.6)',
  },
});
