import { Alert, Linking } from 'react-native';

export function callPhone(phone: string) {
  const digits = phone.replace(/[^0-9+]/g, '');
  if (!digits) return;
  Linking.openURL(`tel:${digits}`).catch(() => {
    Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
  });
}

export function openDirections(name: string, latitude: number, longitude: number) {
  Linking.openURL(`https://map.kakao.com/link/to/${encodeURIComponent(name)},${latitude},${longitude}`).catch(() => {
    Alert.alert('길찾기 실패', '지도를 열 수 없어요.');
  });
}
