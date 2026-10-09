import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  type StyleProp,
  type TextStyle,
  type ViewStyle,
} from 'react-native';
import { colors, font, radius, spacing } from '../theme/theme';

// 화면마다 복붙되어 있던 버튼을 한 곳으로 모은 것.
// 색/높이를 직접 적지 말고 variant로 고른다. 역할마다 색이 하나씩 있다.
//
//   brand        채움 빨강      가입·저장·다음·전송
//   neutral      채움 검정      로그인·검색·인증요청
//   secondary    테두리 회색    취소처럼 brand와 짝으로 놓이는 보조 동작
//   destructive  테두리 빨강    탈퇴처럼 되돌릴 수 없는 동작
//   emergency    채움 빨강      119·긴급 알림 전용. 다른 곳에 쓰지 않는다
//
// destructive를 채우지 않는 것이 이 규칙의 핵심이다. 채우면 저장 버튼과 같은 무게로
// 보여서 "되돌릴 수 없다"는 신호가 사라진다.
//
// brand와 emergency는 지금 같은 빨강이다. 색으로는 119와 저장 버튼이 구분되지 않는다.
// 그래도 variant를 나눠두는 이유는 호출부에 의도가 남기 때문이다 - 나중에 119만
// 따로 떼고 싶어지면 아래 emergency의 색 한 줄만 바꾸면 된다.
//
// "눌 수 없음"에는 두 가지가 있다. disabled는 아직 조건이 안 됐다는 뜻이고,
// done은 이미 끝났다는 뜻이다. 둘 다 눌리지 않지만 done은 회색으로 덮지 않는다 -
// 회색은 "막혔다"로 읽혀서 끝났다는 정보를 지우기 때문이다.
//
// 아래 둘은 역할이 아니라 상태 표시라 규칙서에는 없다.
//   success      채움 초록      인증 완료처럼 이미 끝난 동작
//   text         배경 없음      "나중에 하기"처럼 비켜가는 링크
export type ButtonVariant =
  | 'brand'
  | 'neutral'
  | 'secondary'
  | 'destructive'
  | 'emergency'
  | 'success'
  | 'text';
export type ButtonSize = 'lg' | 'md';

type Props = {
  title: string;
  onPress?: () => void;
  variant?: ButtonVariant;
  size?: ButtonSize;
  disabled?: boolean;
  /** 통신 중. 스피너로 바뀌고 자동으로 눌리지 않는다 */
  loading?: boolean;
  /** 이미 끝난 동작. 눌리지 않지만 색은 그대로 둔다 (인증 완료, 답변 완료) */
  done?: boolean;
  /** 글자 왼쪽에 들어가는 아이콘 */
  icon?: ReactNode;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
};

export default function Button({
  title,
  onPress,
  variant = 'brand',
  size = 'lg',
  disabled = false,
  loading = false,
  done = false,
  icon,
  style,
  textStyle,
}: Props) {
  // 통신 중에 또 누르면 요청이 두 번 나간다. disabled와 같이 묶어둔다.
  const blocked = disabled || loading || done;
  // 테두리형(secondary·destructive)과 text는 배경이 없어서 비활성 처리 방식이 다르다.
  const fill = variant !== 'secondary' && variant !== 'destructive' && variant !== 'text';

  return (
    <Pressable
      onPress={onPress}
      disabled={blocked}
      accessibilityRole="button"
      accessibilityState={{ disabled: blocked, busy: loading }}
      style={({ pressed }) => [
        styles.base,
        styles[size],
        styles[variant],
        // 비활성은 opacity로 흐리게 하지 않는다. 글자 대비가 같이 떨어지기 때문에
        // 배경과 글자색을 따로 바꾼다. done은 덮지 않는다.
        blocked && !done && (fill ? styles.blockedFill : styles.blockedFlat),
        pressed && !blocked && styles.pressed,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator
          size="small"
          color={fill ? colors.white : colors.text}
        />
      ) : (
        <>
          {icon}
          <Text
            style={[
              styles.label,
              size === 'md' && styles.labelMd,
              styles[`${variant}Label`],
              blocked && !done && styles.blockedLabel,
              textStyle,
            ]}
          >
            {title}
          </Text>
        </>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    borderRadius: radius.md,
  },
  lg: { height: 52, paddingHorizontal: spacing.lg },
  md: { height: 44, paddingHorizontal: spacing.md },

  brand: { backgroundColor: colors.primary },
  neutral: { backgroundColor: colors.black },
  emergency: { backgroundColor: colors.danger },
  success: { backgroundColor: colors.success },
  secondary: { backgroundColor: colors.white, borderWidth: 1, borderColor: colors.border },
  destructive: { backgroundColor: colors.white, borderWidth: 1, borderColor: colors.danger },
  text: { backgroundColor: 'transparent' },

  label: { fontSize: font.body, fontWeight: '700' },
  labelMd: { fontSize: font.sub },
  brandLabel: { color: colors.white },
  neutralLabel: { color: colors.white },
  emergencyLabel: { color: colors.white },
  successLabel: { color: colors.white },
  secondaryLabel: { color: colors.text },
  destructiveLabel: { color: colors.danger },
  textLabel: { color: colors.textSub, fontWeight: '600' },

  blockedFill: { backgroundColor: colors.disabled },
  blockedFlat: { opacity: 0.5 },
  blockedLabel: { color: colors.disabledText },

  pressed: { opacity: 0.75 },
});
