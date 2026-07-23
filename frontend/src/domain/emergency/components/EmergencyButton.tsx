import { useRef } from 'react';
import { Animated, Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

const SIZE = 64; // 버튼 지름
const STROKE = 4; // 링 두께
const RADIUS = (SIZE - STROKE) / 2; // 링 반지름
const CIRCUMFERENCE = 2 * Math.PI * RADIUS; // 링 둘레(길이)
const HOLD_MS = 1500; // 눌러야 하는 시간

type Props = {
  onFire: () => void; // 끝까지 눌렀을 때 실행할 함수
};

export default function EmergencyButton({ onFire }: Props) {
  // 진행도 0(안참) ~ 1(꽉참). useRef로 리렌더돼도 값 유지
  const progress = useRef(new Animated.Value(0)).current;

  const start = () => {
    progress.setValue(0); // 처음부터
    Animated.timing(progress, {
      toValue: 1,
      duration: HOLD_MS,
      useNativeDriver: false, // strokeDashoffset은 native driver 불가라 false
    }).start(({ finished }) => {
      if (finished) {
        onFire(); // 중간에 안 끊기고 완주 → 발동!
      }
    });
  };

  const cancel = () => {
    progress.stopAnimation(); // 진행 중이면 멈춤(→ finished=false라 발동 안 됨)
    Animated.timing(progress, {
      toValue: 0,
      duration: 150,
      useNativeDriver: false,
    }).start(); // 링을 부드럽게 원상복구
  };

  // 진행도(0~1)를 링의 strokeDashoffset(꽉참=둘레, 다참=0)으로 변환
  const strokeDashoffset = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [CIRCUMFERENCE, 0],
  });

  return (
    <Pressable onPressIn={start} onPressOut={cancel} style={styles.container}>
      {/* 1) 빨간 원 배경 */}
      <View style={styles.circle} />

      {/* 2) 게이지 링 (원 위에 겹침) */}
      <Svg width={SIZE} height={SIZE} style={styles.ring}>
        {/* 배경 링(반투명) */}
        <Circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          stroke="rgba(255,255,255,0.35)"
          strokeWidth={STROKE}
          fill="none"
        />
        {/* 차오르는 링(흰색) */}
        <AnimatedCircle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          stroke="#fff"
          strokeWidth={STROKE}
          fill="none"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
        />
      </Svg>

      {/* 3) 가운데 119 글자 (맨 위) */}
      <Text style={styles.text}>119</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    width: SIZE,
    height: SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  circle: {
    position: 'absolute',
    width: SIZE,
    height: SIZE,
    borderRadius: SIZE / 2,
    backgroundColor: '#d64533',
    // 그림자
    shadowColor: '#000',
    shadowOpacity: 0.25,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 5,
  },
  ring: {
    position: 'absolute',
    transform: [{ rotate: '-90deg' }], // 시작점을 3시→12시로 돌림
  },
  text: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
});