import * as SecureStore from './secureStore';

// 비회원 AI 상담 식별 코드. 기기 하나에 하나만 두고 모든 상담 세션에서 재사용한다 -
// 서버가 이 값으로 사용 횟수(10회)를 세고, 로그인 후 동의하면 기록을 그 계정으로 옮긴다.
const GUEST_CODE_KEY = 'guestCode';
// 로그인 후 "기록 가져오기"를 거절한 코드 - 같은 코드로 로그인할 때마다 또 묻지 않게.
const DECLINED_KEY = 'guestCodeDeclined';

export async function getGuestCode() {
  return SecureStore.getItemAsync(GUEST_CODE_KEY);
}

export async function saveGuestCode(code: string) {
  await SecureStore.setItemAsync(GUEST_CODE_KEY, code);
}

// 기록을 계정으로 가져온 뒤 호출
export async function clearGuestCode() {
  await SecureStore.deleteItemAsync(GUEST_CODE_KEY);
}

export async function getDeclinedGuestCode() {
  return SecureStore.getItemAsync(DECLINED_KEY);
}

export async function saveDeclinedGuestCode(code: string) {
  await SecureStore.setItemAsync(DECLINED_KEY, code);
}
