import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { MyPageStackParamList } from '../types';
import MyPageHomeScreen from './MyPageHomeScreen';
import GuardianScreen from './GuardianScreen';
import GuardianFormScreen from './GuardianFormScreen';
import HealthScreen from './HealthScreen';


const Stack = createNativeStackNavigator<MyPageStackParamList>();

export default function MyPageNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="MyPageHome" component={MyPageHomeScreen} />
      <Stack.Screen name="Guardian" component={GuardianScreen} />
      <Stack.Screen name="GuardianForm" component={GuardianFormScreen} />
      <Stack.Screen name="Health" component={HealthScreen} />
    </Stack.Navigator>
  );
}