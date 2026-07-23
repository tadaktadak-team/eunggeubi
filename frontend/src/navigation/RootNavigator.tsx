import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import PlaceholderScreen from '../shared/components/PlaceholderScreen';
import TabBar from '../shared/components/TabBar';

export type RootTabParamList = {
  AiChat: undefined;
  Hospital: undefined;
  Medicine: undefined;
  My: undefined;
};

const Tab = createBottomTabNavigator<RootTabParamList>();

export default function RootNavigator() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Tab.Navigator
          tabBar={(props) => <TabBar {...props} />}
          screenOptions={{ headerShown: false }}
        >
          <Tab.Screen name="AiChat" options={{ title: 'AI상담' }}>
            {() => <PlaceholderScreen name="AI상담" />}
          </Tab.Screen>
          <Tab.Screen name="Hospital" options={{ title: '병원찾기' }}>
            {() => <PlaceholderScreen name="병원찾기" />}
          </Tab.Screen>
          <Tab.Screen name="Medicine" options={{ title: '약물정보' }}>
            {() => <PlaceholderScreen name="약물정보" />}
          </Tab.Screen>
          <Tab.Screen name="My" options={{ title: '마이' }}>
            {() => <PlaceholderScreen name="마이" />}
          </Tab.Screen>
        </Tab.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}