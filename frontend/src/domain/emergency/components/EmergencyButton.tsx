import { useState } from 'react';
import { Animated, Pressable, StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';

import { colors } from '../../../shared/theme/theme';

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

const SIZE = 64;
const STROKE = 4;
const RADIUS = (SIZE - STROKE) / 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
const HOLD_MS = 1500;

type Props = {
  onFire: () => void;
};

export default function EmergencyButton({ onFire }: Props) {
  const [progress] = useState(() => new Animated.Value(0));

  const start = () => {
    progress.setValue(0);
    Animated.timing(progress, {
      toValue: 1,
      duration: HOLD_MS,
      useNativeDriver: false, // strokeDashoffset은 native driver 미지원
    }).start(({ finished }) => {
      if (finished) onFire(); // 끝까지 눌러 완주했을 때만 발동
    });
  };

  const cancel = () => {
    progress.stopAnimation();
    Animated.timing(progress, {
      toValue: 0,
      duration: 150,
      useNativeDriver: false,
    }).start();
  };

  const strokeDashoffset = progress.interpolate({
    inputRange: [0, 1],
    outputRange: [CIRCUMFERENCE, 0],
  });

  return (
    <Pressable onPressIn={start} onPressOut={cancel} style={styles.container}>
      <View style={styles.circle} />

      <Svg width={SIZE} height={SIZE} style={styles.ring}>
        <Circle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          stroke="rgba(255,255,255,0.35)"
          strokeWidth={STROKE}
          fill="none"
        />
        <AnimatedCircle
          cx={SIZE / 2}
          cy={SIZE / 2}
          r={RADIUS}
          stroke={colors.white}
          strokeWidth={STROKE}
          fill="none"
          strokeDasharray={CIRCUMFERENCE}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
        />
      </Svg>

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
    backgroundColor: colors.primary,
    shadowColor: '#000',
    shadowOpacity: 0.25,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 5,
  },
  ring: {
    position: 'absolute',
    transform: [{ rotate: '-90deg' }], // 시작점을 12시 방향으로
  },
  text: {
    color: colors.white,
    fontWeight: 'bold',
    fontSize: 16,
  },
});
