import { StyleSheet, Text } from 'react-native';

import { colors, font, spacing } from '../../../shared/theme/theme';

// 두 화면(증상 홈 / AI 챗) 공통으로 쓰는 하단 면책 문구
export default function DisclaimerFooter() {
  return <Text style={styles.text}>※ 참고용이며, 의료 전문가의 상담을 대체하지 않습니다.</Text>;
}

const styles = StyleSheet.create({
  text: {
    textAlign: 'center',
    color: colors.placeholder,
    fontSize: font.caption,
    paddingVertical: spacing.sm,
  },
});
