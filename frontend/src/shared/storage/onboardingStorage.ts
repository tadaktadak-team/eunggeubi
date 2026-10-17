import * as SecureStore from './secureStore';

// 앱을 처음 연 사람에게 한 번만 둘러보기를 보여준다. 건너뛰어도 본 것으로 친다 -
// 다시 보고 싶으면 마이페이지에서 열 수 있게 할 것.
const SEEN_KEY = 'onboardingSeen';

export async function hasSeenOnboarding() {
  return (await SecureStore.getItemAsync(SEEN_KEY)) === '1';
}

export async function markOnboardingSeen() {
  await SecureStore.setItemAsync(SEEN_KEY, '1');
}
