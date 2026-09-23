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

  const callEmergency = () => {
    if (__DEV__) {
      Alert.alert('개발 모드', '실제 배포 앱에서는 119로 전화가 연결됩니다.');
      return;
    }
    Linking.openURL('tel:119').catch(() => {
      Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
    });
  };

  const trigger = async () => {
    if (loading) return;
    setLoading(true);
    try {
      // 비회원은 보호자 알림 없이 전화만 연결
      if (!isLoggedIn) {
        callEmergency();
        return;
      }

      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('위치 권한 필요', '보호자에게 위치를 보내려면 위치 권한이 필요해요.');
        callEmergency();
        return;
      }

      // 마지막으로 알려진 위치가 있으면 바로 사용해 119 연결을 늦추지 않는다
      const pos =
        (await Location.getLastKnownPositionAsync()) ??
        (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
      const { latitude, longitude } = pos.coords;

      // 문자에 주소를 함께 담기 위해 좌표를 먼저 주소로 변환한다
      const geo = await Location.reverseGeocodeAsync({ latitude, longitude }).catch(() => []);
      let address = `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`;
      if (geo.length > 0) {
        const g = geo[0];
        address = [g.city, g.district, g.street, g.name].filter(Boolean).join(' ');
      }

      const result = await sendEmergencyAlert(latitude, longitude, address);

      navigation.navigate('EmergencyResult', {
        address,
        latitude,
        longitude,
        sentAt: result.sentAt,
        message: result.message,
        guardians: result.guardians,
      });

      callEmergency();
    } catch (e) {
      Alert.alert('오류', '긴급 발동 중 문제가 발생했어요.');
      callEmergency();
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return { trigger, loading };
}