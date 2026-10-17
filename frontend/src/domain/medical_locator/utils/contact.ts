import { Alert, Linking } from 'react-native';

import { callNumber } from '../../../shared/utils/call';

export const callPhone = callNumber;

export function openDirections(name: string, latitude: number, longitude: number) {
  Linking.openURL(`https://map.kakao.com/link/to/${encodeURIComponent(name)},${latitude},${longitude}`).catch(() => {
    Alert.alert('길찾기 실패', '지도를 열 수 없어요.');
  });
}
