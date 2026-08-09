import { createNativeStackNavigator } from '@react-navigation/native-stack';

import { AiConsultationStackParamList } from '../types';
import FirstAidGuideScreen from './FirstAidGuideScreen';
import SymptomChatScreen from './SymptomChatScreen';
import SymptomHomeScreen from './SymptomHomeScreen';

const Stack = createNativeStackNavigator<AiConsultationStackParamList>();

// "AI상담" 탭 내부 스택 (증상 입력 홈 → AI 증상 챗 → 응급처치 안내)
export default function AiConsultationNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="SymptomHome" component={SymptomHomeScreen} />
      <Stack.Screen name="SymptomChat" component={SymptomChatScreen} />
      <Stack.Screen name="FirstAidGuide" component={FirstAidGuideScreen} />
    </Stack.Navigator>
  );
}
