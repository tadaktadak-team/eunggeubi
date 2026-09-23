// 카카오맵 JavaScript 키. 클라이언트에 노출되는 값이라 도메인·플랫폼 등록으로 제한한다.
// 환경변수가 있으면 그 값을 쓰고, 없으면 기존에 쓰던 키로 동작한다.
export const KAKAO_JS_KEY =
  process.env.EXPO_PUBLIC_KAKAO_JS_KEY ?? 'd2ca7fd693afc98a1e534caf7e697b48';
