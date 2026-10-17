import { Alert, Linking } from 'react-native';

// 전화 걸기. 전화 기능이 없는 기기(시뮬레이터, 태블릿 등)에서는 실패를 안내한다
export function callNumber(number: string) {
  const digits = number.replace(/[^0-9+]/g, '');
  if (!digits) return;
  Linking.openURL(`tel:${digits}`).catch(() => {
    Alert.alert('전화 연결 실패', '이 기기에서는 전화를 걸 수 없어요.');
  });
}
