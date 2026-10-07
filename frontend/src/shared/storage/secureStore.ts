import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

// expo-secure-store는 웹 구현이 없어서(웹 모듈이 빈 객체라 호출하면 에러) 웹 프리뷰에서는 로그인/가입/
// AI 상담이 전부 깨진다. 네이티브는 SecureStore 그대로, 웹만 localStorage로 대신한다.
// 함수 이름을 SecureStore와 똑같이 맞춰서 쓰는 쪽은 import만 바꾸면 된다.
// ponytail: 웹 localStorage는 암호화가 없고 XSS에 그대로 노출된다 - 웹을 정식 서비스로 열게 되면
// 토큰은 httpOnly 쿠키 방식으로 바꿀 것.
const isWeb = Platform.OS === 'web';

export async function getItemAsync(key: string): Promise<string | null> {
  return isWeb ? localStorage.getItem(key) : SecureStore.getItemAsync(key);
}

export async function setItemAsync(key: string, value: string): Promise<void> {
  if (isWeb) localStorage.setItem(key, value);
  else await SecureStore.setItemAsync(key, value);
}

export async function deleteItemAsync(key: string): Promise<void> {
  if (isWeb) localStorage.removeItem(key);
  else await SecureStore.deleteItemAsync(key);
}
