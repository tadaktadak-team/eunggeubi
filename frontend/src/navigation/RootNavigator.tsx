import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useState } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import AiConsultationNavigator from '../domain/ai_consultations/screens/AiConsultationNavigator';
import { useAuth } from '../domain/auth/hooks/useAuth';
import LoginRequiredSheet from '../domain/auth/components/LoginRequiredSheet';
import FindAccountScreen from '../domain/auth/screens/FindAccountScreen';
import LoginScreen from '../domain/auth/screens/LoginScreen';
import SignupCompleteScreen from '../domain/auth/screens/SignupCompleteScreen';
import SignupScreen from '../domain/auth/screens/SignupScreen';
import SplashScreen from '../domain/auth/screens/SplashScreen';
import DrugNavigator from '../domain/drug/screens/DrugNavigator';
import EmergencyResultScreen from '../domain/emergency/screens/EmergencyResultScreen';
import MedicalLocatorScreen from '../domain/medical_locator/screens/MedicalLocatorScreen';
import MyPageNavigator from '../domain/mypage/screens/MyPageNavigator';
import TabBar from '../shared/components/TabBar';
import LegalScreen from '../shared/screens/LegalScreen';
import { RootStackParamList } from './types';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator<RootStackParamList>();

function TabsNavigator() {
  const { isLoggedIn } = useAuth();
  const [guestSheetVisible, setGuestSheetVisible] = useState(!isLoggedIn);

  return (
    <>
      <Tab.Navigator tabBar={(props) => <TabBar {...props} />} screenOptions={{ headerShown: false }}>
        <Tab.Screen name="AiChat" component={AiConsultationNavigator} options={{ title: 'AI상담' }} />
        <Tab.Screen name="Hospital" component={MedicalLocatorScreen} options={{ title: '병원찾기' }} />
        <Tab.Screen name="Medicine" component={DrugNavigator} options={{ title: '약물정보' }} />
        <Tab.Screen name="My" component={MyPageNavigator} options={{ title: '마이' }} />
      </Tab.Navigator>

      <LoginRequiredSheet visible={guestSheetVisible} onClose={() => setGuestSheetVisible(false)} />
    </>
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
              <Stack.Screen name="FindAccount" component={FindAccountScreen} />
              <Stack.Screen name="Tabs" component={TabsNavigator} />
              <Stack.Screen name="EmergencyResult" component={EmergencyResultScreen} />
              <Stack.Screen name="Legal" component={LegalScreen} />
            </>
          ) : (
            <>
              <Stack.Screen name="Tabs" component={TabsNavigator} />
              <Stack.Screen name="EmergencyResult" component={EmergencyResultScreen} />
              <Stack.Screen name="Legal" component={LegalScreen} />
            </>
          )}
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}