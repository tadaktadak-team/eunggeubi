import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import EmergencyResultScreen from '../domain/emergency/screens/EmergencyResultScreen';
import PlaceholderScreen from '../shared/components/PlaceholderScreen';
import TabBar from '../shared/components/TabBar';
import { RootStackParamList } from './types';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator<RootStackParamList>();

function TabsNavigator() {
  return (
    <Tab.Navigator tabBar={(props) => <TabBar {...props} />} screenOptions={{ headerShown: false }}>
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
  );
}

export default function RootNavigator() {
  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="Tabs" component={TabsNavigator} options={{ headerShown: false }} />
          <Stack.Screen
            name="EmergencyResult"
            component={EmergencyResultScreen}
            options={{ headerShown: false }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}