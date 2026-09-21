import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { MyPageStackParamList } from '../types';
import MyPageHomeScreen from './MyPageHomeScreen';
import GuardianScreen from './GuardianScreen';
import GuardianFormScreen from './GuardianFormScreen';
import HealthScreen from './HealthScreen';
import ChangePasswordScreen from './ChangePasswordScreen';
import AccountEditScreen from './AccountEditScreen';
import WithdrawScreen from './WithdrawScreen';
import ConsultationHistoryScreen from './ConsultationHistoryScreen';
import ConsultationDetailScreen from './ConsultationDetailScreen';
import SupportScreen from './SupportScreen';


const Stack = createNativeStackNavigator<MyPageStackParamList>();

export default function MyPageNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="MyPageHome" component={MyPageHomeScreen} />
      <Stack.Screen name="Guardian" component={GuardianScreen} />
      <Stack.Screen name="GuardianForm" component={GuardianFormScreen} />
      <Stack.Screen name="Health" component={HealthScreen} />
      <Stack.Screen name="ChangePassword" component={ChangePasswordScreen} />
      <Stack.Screen name="AccountEdit" component={AccountEditScreen} />
      <Stack.Screen name="Withdraw" component={WithdrawScreen} />
      <Stack.Screen name="ConsultationHistory" component={ConsultationHistoryScreen} />
      <Stack.Screen name="ConsultationDetail" component={ConsultationDetailScreen} />
      <Stack.Screen name="Support" component={SupportScreen} />
    </Stack.Navigator>
  );
}