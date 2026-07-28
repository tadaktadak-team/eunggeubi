import * as Location from 'expo-location';
import { useState } from 'react';
import { Alert, Linking } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';

import { useAuth } from '../../auth/hooks/useAuth';
import { RootStackParamList } from '../../../navigation/types';
import { sendEmergencyAlert } from '../api/emergency';

export function useEmergency() {
  const [loading, setLoading] = useState(false);
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { isLoggedIn } = useAuth();

  const trigger = async () => {
    if (loading) return;
    setLoading(true);
    try {
      // 119 전화는 로그인 여부와 상관없이 항상 연결
      Linking.openURL('tel:119').catch(() => {
        Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
      });

      // 비회원은 전화만 연결하고 종료 (보호자 알림 안내는 앱 진입 시 처리)
      if (!isLoggedIn) {
        return;
      }

      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('위치 권한 필요', '보호자에게 위치를 보내려면 위치 권한이 필요해요.');
        return;
      }

      const pos = await Location.getCurrentPositionAsync({});
      const { latitude, longitude } = pos.coords;

      let address = `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`;
      const geo = await Location.reverseGeocodeAsync({ latitude, longitude });
      if (geo.length > 0) {
        const g = geo[0];
        address = [g.city, g.district, g.street, g.name].filter(Boolean).join(' ');
      }

      const result = await sendEmergencyAlert(latitude, longitude);

      navigation.navigate('EmergencyResult', {
        address,
        sentAt: result.sentAt,
        guardians: result.guardians,
      });
    } catch (e) {
      Alert.alert('오류', '긴급 발동 중 문제가 발생했어요.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return { trigger, loading };
}