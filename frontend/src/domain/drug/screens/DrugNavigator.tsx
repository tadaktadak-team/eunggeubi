import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import DrugHomeScreen from './DrugHomeScreen';
import DrugDetailScreen from './DrugDetailScreen';
import PillSearchScreen from './PillSearchScreen';
import InteractionCheckScreen from './InteractionCheckScreen';

const Stack = createNativeStackNavigator();

export default function DrugNavigator() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="DrugHome" component={DrugHomeScreen} />
      <Stack.Screen name="DrugDetail" component={DrugDetailScreen} />
      <Stack.Screen name="PillSearch" component={PillSearchScreen} />
      <Stack.Screen name="InteractionCheck" component={InteractionCheckScreen} />
    </Stack.Navigator>
  );
}