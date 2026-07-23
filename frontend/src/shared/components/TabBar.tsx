import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import EmergencyButton from '../../domain/emergency/components/EmergencyButton';
import { useEmergency } from '../../domain/emergency/hooks/useEmergency';

const ICONS: Record<string, { lib: 'ion' | 'mci'; active: string; inactive: string }> = {
  AiChat: { lib: 'ion', active: 'chatbubble-ellipses', inactive: 'chatbubble-ellipses-outline' },
  Hospital: { lib: 'ion', active: 'location', inactive: 'location-outline' },
  Medicine: { lib: 'mci', active: 'pill', inactive: 'pill' },
  My: { lib: 'ion', active: 'person', inactive: 'person-outline' },
};

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
  const { trigger } = useEmergency();

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
        <View style={styles.group}>
          {state.routes.slice(0, 2).map((route, i) => renderTab(route, i))}
        </View>

        <View style={styles.centerSlot} />

        <View style={styles.group}>
          {state.routes.slice(2, 4).map((route, i) => renderTab(route, i + 2))}
        </View>
      </View>

      <View style={styles.fabWrapper}>
        <EmergencyButton onFire={trigger} />
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
    marginLeft: -32,
  },
});
