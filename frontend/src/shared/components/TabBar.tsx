import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import EmergencyButton from '../../domain/emergency/components/EmergencyButton';
// 탭 아이콘. lib = 어느 아이콘 세트인지(ion=Ionicons, mci=MaterialCommunityIcons)
const ICONS: Record<string, { lib: 'ion' | 'mci'; active: string; inactive: string }> = {
  AiChat: { lib: 'ion', active: 'chatbubble-ellipses', inactive: 'chatbubble-ellipses-outline' },
  Hospital: { lib: 'ion', active: 'location', inactive: 'location-outline' },
  Medicine: { lib: 'mci', active: 'pill', inactive: 'pill' },
  My: { lib: 'ion', active: 'person', inactive: 'person-outline' },
};

// 아이콘 하나 그려주는 작은 컴포넌트 (Ionicons / MaterialCommunityIcons 둘 다 처리)
function TabIcon({ routeName, focused, color }: { routeName: string; focused: boolean; color: string }) {
  const icon = ICONS[routeName];
  if (!icon) return null;
  const name = focused ? icon.active : icon.inactive;
  return icon.lib === 'mci' ? (
    <MaterialCommunityIcons name={name as any} size={22} color={color} />
  ) : (
    <Ionicons name={name as any} size={22} color={color} />
  );
}

export default function TabBar({ state, descriptors, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();

  const renderTab = (route: (typeof state.routes)[number], index: number) => {
    const { options } = descriptors[route.key];
    const label = options.title ?? route.name;
    const isFocused = state.index === index;
    const color = isFocused ? '#d64533' : '#999';

    const onPress = () => {
      const event = navigation.emit({
        type: 'tabPress',
        target: route.key,
        canPreventDefault: true,
      });
      if (!isFocused && !event.defaultPrevented) {
        navigation.navigate(route.name);
      }
    };

    return (
      <Pressable key={route.key} style={styles.tab} onPress={onPress}>
        <TabIcon routeName={route.name} focused={isFocused} color={color} />
        <Text style={[styles.label, isFocused && styles.labelFocused]}>{label}</Text>
      </Pressable>
    );
  };

  return (
    <View style={[styles.wrapper, { paddingBottom: insets.bottom }]}>
      <View style={styles.bar}>
        {/* 왼쪽 2개 */}
        <View style={styles.group}>
          {state.routes.slice(0, 2).map((route, i) => renderTab(route, i))}
        </View>

        {/* 가운데 119 자리 */}
        <View style={styles.centerSlot} />

        {/* 오른쪽 2개 */}
        <View style={styles.group}>
          {state.routes.slice(2, 4).map((route, i) => renderTab(route, i + 2))}
        </View>
      </View>

    {/* 중앙 119 버튼 (길게 누르면 발동) */}
      <View style={styles.fabWrapper}>
        <EmergencyButton onFire={() => console.log('🚨 발동! (3단계에서 전화+알림 연결)')} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: { backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#eee' },
  bar: { flexDirection: 'row', height: 60, alignItems: 'center' },
  group: { flex: 1, flexDirection: 'row' },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  centerSlot: { width: 80 },
  label: { fontSize: 12, color: '#999', marginTop: 2 },
  labelFocused: { color: '#d64533', fontWeight: 'bold' },
  fabWrapper: {
    position: 'absolute',
    top: -10,
    left: '50%',
    marginLeft: -32, // 버튼 너비(64) 절반
  },
});