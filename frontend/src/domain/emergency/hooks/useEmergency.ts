import * as Location from 'expo-location';
import { useState } from 'react';
import { Alert, Linking } from 'react-native';

import { sendEmergencyAlert } from '../api/emergency';

export function useEmergency() {
  const [loading, setLoading] = useState(false);

  const trigger = async () => {
    if (loading) return;
    setLoading(true);
    try {
      Linking.openURL('tel:119').catch(() => {
        Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
      });

      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('위치 권한 필요', '보호자에게 위치를 보내려면 위치 권한이 필요해요.');
        return;
      }

      const pos = await Location.getCurrentPositionAsync({});
      const { latitude, longitude } = pos.coords;

      // 좌표 → 사람이 읽는 주소 (실패 시 좌표 그대로 사용)
      let address = `${latitude.toFixed(5)}, ${longitude.toFixed(5)}`;
      const geo = await Location.reverseGeocodeAsync({ latitude, longitude });
      if (geo.length > 0) {
        const g = geo[0];
        address = [g.city, g.district, g.street, g.name].filter(Boolean).join(' ');
      }

      const result = await sendEmergencyAlert(latitude, longitude);

      // TODO: 4단계에서 결과 화면 이동으로 교체
      Alert.alert(
        '보호자에게 알림 발송됨',
        `현재 위치: ${address}\n발송 대상: ${result.guardians.map((x) => x.name).join(', ')}`,
      );
    } catch (e) {
      Alert.alert('오류', '긴급 발동 중 문제가 발생했어요.');
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  return { trigger, loading };
}
