import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, Easing, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import type { ViewInstance } from 'react-native';
import Svg, { Path } from 'react-native-svg';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';

import Button from '../../../shared/components/Button';
import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import EmergencyButton from '../../emergency/components/EmergencyButton';

// 앱을 처음 연 사람에게 보여주는 둘러보기.
//
// 홈 화면을 여기에 다시 그린다. 진짜 홈 위에 덮지 않는 이유는, 둘러보기가 뜨는 시점에
// 응급실 목록이 아직 안 왔을 수 있어서다 - 빈 자리를 가리키게 되면 설명이 무너진다.
// 대신 수치는 홈과 같은 토큰을 쓰고, 119 버튼은 진짜 EmergencyButton을 그대로 쓴다.
//
// 스포트라이트 위치는 좌표를 박지 않고 measureInWindow로 실측한다. 기기 크기가 달라도
// 그려진 자리에 정확히 올라간다.

type Spot = { x: number; y: number; w: number; h: number; r: number };

// 어둡게 덮으면서 한 군데만 뚫는다. 사각형 네 장으로 둘러싸면 구멍이 늘 네모라
// 119처럼 둥근 버튼에서 모서리가 각지게 남는다. 바깥 사각형과 안쪽 둥근 사각형을
// 한 path에 넣고 evenodd로 칠하면 모양 그대로 뚫린다.
function scrimPath(screenW: number, screenH: number, s: Spot) {
  const r = Math.min(s.r, s.w / 2, s.h / 2);
  const { x, y, w, h } = s;
  const hole =
    `M${x + r} ${y} H${x + w - r} A${r} ${r} 0 0 1 ${x + w} ${y + r}` +
    ` V${y + h - r} A${r} ${r} 0 0 1 ${x + w - r} ${y + h}` +
    ` H${x + r} A${r} ${r} 0 0 1 ${x} ${y + h - r}` +
    ` V${y + r} A${r} ${r} 0 0 1 ${x + r} ${y} Z`;
  return `M0 0 H${screenW} V${screenH} H0 Z ` + hole;
}

type Step = {
  key: string;
  title: string;
  body: string;
  cta: string;
  /** 꾹 눌러야 넘어가는 단계. 119만 해당 */
  hold?: boolean;
};

// 탭 아이콘처럼 작고 네모난 대상은 네모로 뚫으면 둔해 보인다.
// 아이콘 중심을 기준으로 동그랗게 뚫는다.
const CIRCLE_SPOT = 60;

// 탭바 높이. 카드를 아래에 둘 때 탭바를 덮지 않도록 띄우는 데 쓴다.
const TABBAR_H = 60;

const STEPS: Step[] = [
  {
    key: 'symptom',
    title: '증상을 그대로 적어보세요',
    body: '질병관리청 자료를 근거로 안내하고 출처를 함께 보여줍니다. 진단은 하지 않아요.',
    cta: '다음',
  },
  {
    key: 'er',
    title: '가까운 응급실이 먼저 보입니다',
    body: '지금 갈 수 있는 곳과 남은 병상을 홈에서 바로 확인해요. 탭하면 전화와 길찾기가 열립니다.',
    cta: '다음',
  },
  {
    key: 'drug',
    title: '먹는 약이 겹치는지 봐드려요',
    body: '약 이름이나 낱알 모양으로 찾고, 두 개 이상 고르면 같이 먹어도 되는지 식약처 기준으로 확인해요.',
    cta: '다음',
  },
  {
    key: 'sos',
    title: '119는 꾹 눌러야 걸립니다',
    body: '한 번 해볼까요? 아래 버튼을 누르고 계시면 테두리가 한 바퀴 돕니다. 중간에 떼면 취소돼요.',
    cta: '다음',
    hold: true,
  },
  {
    key: 'my',
    title: '보호자를 등록해두세요',
    body: '긴급 호출을 하면 등록된 보호자에게 상황과 현재 위치가 문자로 갑니다. 로그인 후 마이에서 등록할 수 있어요.',
    cta: '시작하기',
  },
];

const CHIPS = ['두통', '복통', '발열', '어지럼'];
const SHORTCUTS: { label: string; icon: keyof typeof Ionicons.glyphMap }[] = [
  { label: '병원찾기', icon: 'location-outline' },
  { label: '응급처치 안내', icon: 'alert-circle-outline' },
  { label: '약물정보', icon: 'medkit-outline' },
];

// 한 글자씩 찍는 속도. 제목과 본문을 이어붙인 길이를 기준으로 센다.
const TYPE_STEP = 2;
const TYPE_MS = 34;

// 1단계에서 증상칸에 누가 직접 적는 것처럼 보여준다. 안내 문구보다 느리게 찍어야
// "사람이 입력 중"으로 읽힌다.
const DEMO_TEXT = '어젯밤부터 두통이 심해요';
const DEMO_MS = 70;
const DEMO_HOLD_MS = 900;

type Props = { onDone: () => void };

export default function OnboardingScreen({ onDone }: Props) {
  const insets = useSafeAreaInsets();
  // useWindowDimensions는 실제 뷰포트와 어긋나는 경우가 있어(웹 프리뷰에서 확인) 막이
  // 화면을 다 못 덮는다. 루트가 실제로 차지한 크기를 직접 잰다.
  const [frame, setFrame] = useState({ w: 0, h: 0 });
  const screenW = frame.w;
  const screenH = frame.h;
  const [step, setStep] = useState(0);
  const [typed, setTyped] = useState(0);
  const [spots, setSpots] = useState<Record<string, Spot>>({});

  const current = STEPS[step];
  const spot = spots[current.key];

  // measureInWindow는 레이아웃이 끝난 뒤에야 값이 나온다. onLayout에서 불러 담아둔다.
  const measure = useCallback((key: string, node: ViewInstance | null, circle = false) => {
    node?.measureInWindow((x, y, w, h) => {
      if (!w || !h) return;
      // 원형이면 요소 한가운데에 고정 지름의 원을 올린다
      const next: Spot = circle
        ? {
            x: x + w / 2 - CIRCLE_SPOT / 2,
            y: y + h / 2 - CIRCLE_SPOT / 2,
            w: CIRCLE_SPOT,
            h: CIRCLE_SPOT,
            r: 999,
          }
        : { x, y, w, h, r: radius.md };
      setSpots((prev) => {
        const p = prev[key];
        if (p && p.x === next.x && p.y === next.y && p.w === next.w && p.h === next.h) return prev;
        return { ...prev, [key]: next };
      });
    });
  }, []);

  // 구멍 테두리가 천천히 퍼졌다 사라진다. 어디를 보라는 건지 눈에 먼저 들어오게.
  // EmergencyButton과 같은 방식. useRef().current는 렌더 중 ref 접근이라 린트가 막는다.
  const [pulse] = useState(() => new Animated.Value(0));
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, {
          toValue: 1,
          duration: 1600,
          easing: Easing.out(Easing.ease),
          // 웹에서는 native driver로 돌리면 한 바퀴 돌고 끝값에 멈춘다. 네이티브에서만 켠다.
          useNativeDriver: Platform.OS !== 'web',
        }),
        Animated.timing(pulse, { toValue: 0, duration: 0, useNativeDriver: Platform.OS !== 'web' }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [pulse]);

  const symptomRef = useRef<ViewInstance>(null);
  const erRef = useRef<ViewInstance>(null);
  const drugRef = useRef<ViewInstance>(null);
  const sosRef = useRef<ViewInstance>(null);
  const myRef = useRef<ViewInstance>(null);

  // 증상칸 데모 타이핑. 1단계에서만 돈다.
  const [demo, setDemo] = useState(0);
  const onSymptomStep = current.key === 'symptom';

  useEffect(() => {
    if (!onSymptomStep) return;
    // 다 찍으면 잠시 두었다가 지우고 다시 찍는다 - 뒤늦게 본 사람도 한 번은 보게.
    const delay = demo > DEMO_TEXT.length ? DEMO_HOLD_MS : DEMO_MS;
    const t = setTimeout(() => setDemo((v) => (v > DEMO_TEXT.length ? 0 : v + 1)), delay);
    return () => clearTimeout(t);
  }, [demo, onSymptomStep]);

  // 입력 커서 깜박임
  const [blink] = useState(() => new Animated.Value(0));
  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(blink, { toValue: 1, duration: 0, delay: 500, useNativeDriver: Platform.OS !== 'web' }),
        Animated.timing(blink, { toValue: 0, duration: 0, delay: 500, useNativeDriver: Platform.OS !== 'web' }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [blink]);

  const total = current.title.length + current.body.length;

  useEffect(() => {
    if (typed >= total) return;
    const t = setTimeout(() => setTyped((v) => Math.min(total, v + TYPE_STEP)), TYPE_MS);
    return () => clearTimeout(t);
  }, [typed, total]);

  const advance = useCallback(() => {
    if (step >= STEPS.length - 1) {
      onDone();
      return;
    }
    // 글자는 단계를 넘기는 이 자리에서 되감는다. step을 보는 effect로 하면
    // 렌더가 한 번 더 돌아 첫 글자가 늦게 찍힌다.
    setStep(step + 1);
    setTyped(0);
  }, [step, onDone]);

  // 카드는 위·아래 두 자리만 쓴다. 매 단계 따라다니면 눈이 피로하고, 한 자리에 못 박으면
  // 가운데쯤 있는 응급실 카드를 가린다. 가리키는 곳이 화면 아래쪽이면 위로 올라간다.
  const dockTop = spot ? spot.y + spot.h / 2 > screenH * 0.55 : false;

  const shownTitle = current.title.slice(0, Math.min(typed, current.title.length));
  const shownBody = typed > current.title.length ? current.body.slice(0, typed - current.title.length) : '';

  return (
    <View
      style={styles.root}
      onLayout={(e) => {
        const { width, height } = e.nativeEvent.layout;
        setFrame((f) => (f.w === width && f.h === height ? f : { w: width, h: height }));
      }}
    >
      {/* ───── 홈 화면 재현 ───── */}
      <SafeAreaView style={styles.flex} edges={['top']}>
        <View style={styles.body}>
          <Text style={styles.h1}>어디가{'\n'}불편하세요?</Text>
          <Text style={styles.lead}>증상을 말하면 AI가 참고정보를 안내해요</Text>

          <View ref={symptomRef} onLayout={() => measure('symptom', symptomRef.current)} style={styles.askBox}>
            {onSymptomStep ? (
              <Text style={styles.askTyped}>
                {DEMO_TEXT.slice(0, Math.min(demo, DEMO_TEXT.length))}
                <Animated.Text style={[styles.caret, { opacity: blink }]}>|</Animated.Text>
              </Text>
            ) : (
              <Text style={styles.askPlaceholder}>예) “어젯밤부터 두통이 심해요”</Text>
            )}
            <View style={styles.askBtn}>
              <Text style={styles.askBtnText}>물어보기</Text>
            </View>
          </View>

          <View style={styles.chipRow}>
            {CHIPS.map((c) => (
              <View key={c} style={styles.chip}>
                <Text style={styles.chipText}>{c}</Text>
              </View>
            ))}
          </View>

          <Text style={styles.sectionTitle}>바로가기</Text>
          <View style={styles.shortcutRow}>
            {SHORTCUTS.map((s) => (
              <View key={s.label} style={styles.shortcut}>
                <Ionicons name={s.icon} size={20} color={colors.textSub} />
                <Text style={styles.shortcutText}>{s.label}</Text>
              </View>
            ))}
          </View>

          <Text style={styles.sectionTitle}>가까운 응급실</Text>
          <View ref={erRef} onLayout={() => measure('er', erRef.current)} style={styles.erCard}>
            <View style={styles.flex}>
              <Text style={styles.erName}>서울대학교병원</Text>
              <Text style={styles.erSub}>2.1km · 혼잡도 낮음</Text>
            </View>
            <View style={styles.erBadge}>
              <Text style={styles.erBadgeText}>병상 12</Text>
            </View>
          </View>
        </View>
      </SafeAreaView>

      {/* ───── 탭바 재현. 수치는 shared/components/TabBar와 같게 맞춘다 ───── */}
      <View style={[styles.tabWrapper, { paddingBottom: insets.bottom }]}>
        <View style={styles.tabBar}>
          <View style={styles.tabGroup}>
            <View style={styles.tab}>
              <Ionicons name="chatbubble-ellipses" size={22} color={colors.primary} />
              <Text style={[styles.tabLabel, styles.tabLabelOn]}>AI상담</Text>
            </View>
            <View style={styles.tab}>
              <Ionicons name="location-outline" size={22} color={colors.placeholder} />
              <Text style={styles.tabLabel}>병원찾기</Text>
            </View>
          </View>

          <View style={styles.centerSlot} />

          <View style={styles.tabGroup}>
            <View ref={drugRef} onLayout={() => measure('drug', drugRef.current, true)} style={styles.tab}>
              <MaterialCommunityIcons name="pill" size={22} color={colors.placeholder} />
              <Text style={styles.tabLabel}>약물정보</Text>
            </View>
            <View ref={myRef} onLayout={() => measure('my', myRef.current, true)} style={styles.tab}>
              <Ionicons name="person-outline" size={22} color={colors.placeholder} />
              <Text style={styles.tabLabel}>마이</Text>
            </View>
          </View>
        </View>

        <View ref={sosRef} onLayout={() => measure('sos', sosRef.current, true)} style={styles.fabWrapper}>
          <EmergencyButton onFire={() => {}} />
        </View>
      </View>

      {/* ───── 어둡게 덮고 한 군데만 뚫기 ───── */}
      {spot && screenW > 0 && (
        <>
          <Svg style={StyleSheet.absoluteFill} width={screenW} height={screenH} pointerEvents="none">
            <Path d={scrimPath(screenW, screenH, spot)} fill={colors.text} fillOpacity={0.66} fillRule="evenodd" />
          </Svg>

          <Animated.View
            pointerEvents="none"
            style={[
              styles.ring,
              {
                left: spot.x - 3,
                top: spot.y - 3,
                width: spot.w + 6,
                height: spot.h + 6,
                borderRadius: spot.r + 3,
                opacity: pulse.interpolate({ inputRange: [0, 0.7, 1], outputRange: [0.55, 0, 0] }),
                transform: [{ scale: pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.35] }) }],
              },
            ]}
          />

          <View
            pointerEvents="none"
            style={[
              styles.ring,
              { left: spot.x - 3, top: spot.y - 3, width: spot.w + 6, height: spot.h + 6, borderRadius: spot.r + 3 },
            ]}
          />
        </>
      )}

      {/* 119 단계에서는 진짜 버튼을 구멍 위에 한 번 더 올려 실제로 누를 수 있게 한다 */}
      {current.hold && spot && (
        <View style={[styles.holdLayer, { left: spot.x, top: spot.y, width: spot.w, height: spot.h }]}>
          <EmergencyButton onFire={advance} />
        </View>
      )}

      {/* ───── 안내 카드. 단계가 바뀌어도 자리는 그대로 둔다 ───── */}
      <View style={[styles.card, dockTop ? { top: insets.top + spacing.lg } : { bottom: insets.bottom + TABBAR_H + spacing.lg }]} pointerEvents="box-none">
        <Text style={styles.cardTitle}>{shownTitle}</Text>
        <Text style={styles.cardBody}>{shownBody}</Text>

        <View style={styles.dots}>
          {STEPS.map((s, i) => (
            <View key={s.key} style={[styles.dot, i === step && styles.dotOn]} />
          ))}
        </View>

        <View style={styles.cardActions}>
          <Pressable onPress={onDone} hitSlop={8} style={styles.skip}>
            <Text style={styles.skipText}>건너뛰기</Text>
          </Pressable>

          {current.hold ? (
            <View style={styles.holdHint}>
              <Text style={styles.holdHintText}>아래 119 버튼을 꾹 누르세요</Text>
            </View>
          ) : (
            <Button title={current.cta} size="md" onPress={advance} style={styles.flex} />
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  flex: { flex: 1 },
  body: { flex: 1, paddingHorizontal: spacing.lg, paddingTop: spacing.lg, paddingBottom: spacing.md, gap: spacing.md },

  h1: { fontSize: font.h2, fontWeight: '800', color: colors.text, lineHeight: 30 },
  lead: { fontSize: font.caption, color: colors.textSub, marginTop: -spacing.sm },

  askBox: {
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    padding: spacing.md,
    gap: spacing.md,
  },
  askPlaceholder: { fontSize: font.sub, color: colors.placeholder },
  // 찍히는 글자는 placeholder가 아니라 실제 입력처럼 본문 색으로 둔다
  askTyped: { fontSize: font.sub, color: colors.text },
  caret: { color: colors.primary, fontWeight: '700' },
  askBtn: {
    alignSelf: 'flex-end',
    backgroundColor: colors.black,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.sm,
    borderRadius: radius.pill,
  },
  askBtnText: { color: colors.white, fontSize: font.caption, fontWeight: '700' },

  chipRow: { flexDirection: 'row', gap: spacing.sm },
  chip: { borderWidth: 1, borderColor: colors.border, borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: spacing.xs },
  chipText: { fontSize: font.caption, color: colors.text },

  sectionTitle: { fontSize: font.h3, fontWeight: '700', color: colors.text },

  shortcutRow: { flexDirection: 'row', gap: spacing.sm },
  shortcut: {
    flex: 1,
    backgroundColor: colors.inputBg,
    borderRadius: radius.md,
    height: 62,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
  },
  shortcutText: { fontSize: font.caption, color: colors.textSub },

  erCard: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  erName: { fontSize: font.sub, fontWeight: '700', color: colors.text },
  erSub: { fontSize: font.caption, color: colors.textSub, marginTop: 2 },
  erBadge: { borderWidth: 1, borderColor: colors.danger, borderRadius: radius.pill, paddingHorizontal: spacing.sm, paddingVertical: 2 },
  erBadgeText: { fontSize: font.caption, fontWeight: '700', color: colors.danger },

  tabWrapper: { backgroundColor: colors.white, borderTopWidth: 1, borderTopColor: colors.border },
  tabBar: { flexDirection: 'row', height: 60, alignItems: 'center' },
  tabGroup: { flex: 1, flexDirection: 'row' },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  centerSlot: { width: 80 },
  tabLabel: { fontSize: 12, color: colors.placeholder, marginTop: 2 },
  tabLabelOn: { color: colors.primary, fontWeight: 'bold' },
  fabWrapper: { position: 'absolute', top: -10, left: '50%', marginLeft: -32 },

  ring: { position: 'absolute', borderWidth: 3, borderColor: colors.white },
  holdLayer: { position: 'absolute', alignItems: 'center', justifyContent: 'center' },

  card: {
    position: 'absolute',
    left: spacing.lg,
    right: spacing.lg,
    backgroundColor: colors.white,
    borderRadius: radius.lg,
    padding: spacing.lg,
    gap: spacing.sm,
  },
  // 글자가 찍히는 동안 카드 높이가 변하지 않도록 두 줄·세 줄 자리를 미리 잡아둔다
  cardTitle: { fontSize: font.h3, fontWeight: '800', color: colors.text, lineHeight: 24, minHeight: 48 },
  cardBody: { fontSize: font.sub, color: colors.textSub, lineHeight: 20, minHeight: 60 },

  dots: { flexDirection: 'row', justifyContent: 'center', gap: spacing.xs },
  dot: { width: 7, height: 7, borderRadius: radius.pill, backgroundColor: colors.border },
  dotOn: { width: 20, backgroundColor: colors.primary },

  cardActions: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm, marginTop: spacing.xs },
  skip: { paddingVertical: spacing.md, paddingHorizontal: spacing.sm },
  skipText: { color: colors.textSub, fontSize: font.sub },
  holdHint: {
    flex: 1,
    height: 44,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  holdHintText: { fontSize: font.sub, fontWeight: '700', color: colors.textSub },
});
