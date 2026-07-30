import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import MyScreen from '../domain/user/screens/MyScreen';
import FindAccountScreen from '../domain/auth/screens/FindAccountScreen';
import { useAuth } from '../domain/auth/hooks/useAuth';
import LoginScreen from '../domain/auth/screens/LoginScreen';
import SignupCompleteScreen from '../domain/auth/screens/SignupCompleteScreen';
import SignupScreen from '../domain/auth/screens/SignupScreen';
import SplashScreen from '../domain/auth/screens/SplashScreen';
import EditProfileScreen from '../domain/user/screens/EditProfileScreen';
import GuardianConsentScreen from '../domain/auth/screens/GuardianConsentScreen';
import GuardianWaitingScreen from '../domain/auth/screens/GuardianWaitingScreen';
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
      <Tab.Screen name="My" component={MyScreen} options={{ title: '마이' }} />
    </Tab.Navigator>
  );
}

export default function RootNavigator() {
  const { loading, isLoggedIn } = useAuth();

  return (
    <SafeAreaProvider>
      <NavigationContainer>
        <Stack.Navigator screenOptions={{ headerShown: false }}>
          {loading ? (
            <Stack.Screen name="Splash" component={SplashScreen} />
          ) : !isLoggedIn ? (
            <>
              <Stack.Screen name="Login" component={LoginScreen} />
              <Stack.Screen name="Signup" component={SignupScreen} />
              <Stack.Screen name="SignupComplete" component={SignupCompleteScreen} />
              <Stack.Screen name="GuardianConsent" component={GuardianConsentScreen} />
              <Stack.Screen name="GuardianWaiting" component={GuardianWaitingScreen} />
              <Stack.Screen name="FindAccount" component={FindAccountScreen} />
              <Stack.Screen name="Tabs" component={TabsNavigator} />
              <Stack.Screen name="EmergencyResult" component={EmergencyResultScreen} />
            </>
          ) : (
            <>
              <Stack.Screen name="Tabs" component={TabsNavigator} />
              <Stack.Screen name="EditProfile" component={EditProfileScreen} />
              <Stack.Screen name="EmergencyResult" component={EmergencyResultScreen} />
            </>
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}