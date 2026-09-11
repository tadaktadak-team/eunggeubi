import { StyleSheet, Text } from 'react-native';

import { colors, font, spacing } from '../../../shared/theme/theme';

type Props = {
  // AI 챗 화면처럼 실제로 특정 자료를 근거로 답하는 화면에서만 넘긴다 - 증상 홈/응급처치 안내는
  // 이 출처를 안 쓰므로 안 넘기면 출처 표기 없이 면책 문구만 보여준다.
  sourceName?: string;
};

// 세 화면(증상 홈 / AI 챗 / 응급처치 안내) 공통으로 쓰는 하단 면책 문구.
export default function DisclaimerFooter({ sourceName }: Props) {
  return (
    <Text style={styles.text}>
      ※ 참고용이며, 의료 전문가의 상담을 대체하지 않습니다.
      {!!sourceName && `\n(${sourceName} 자료 기반)`}
    </Text>
  );
}

const styles = StyleSheet.create({
  text: {
    textAlign: 'center',
    color: colors.placeholder,
    fontSize: font.caption,
    paddingVertical: spacing.sm,
  },
});
