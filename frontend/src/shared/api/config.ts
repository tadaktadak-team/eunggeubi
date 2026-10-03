import Constants from 'expo-constants';

/**
 * API 서버 주소를 결정한다.
 *   1순위  .env 의 EXPO_PUBLIC_API_BASE_URL  (운영 서버를 볼 때)
 *   2순위  Metro 가 돌고 있는 PC 의 IP        (로컬 개발 - 팀원마다 IP 가 달라도 설정 불필요)
 *   3순위  localhost                         (웹 프리뷰)
 */
function resolve(): string {
  const fromEnv = process.env.EXPO_PUBLIC_API_BASE_URL;
  if (fromEnv) return fromEnv.replace(/\/$/, '');

  // hostUri 예: "192.168.0.5:8081" - Expo 가 알려주는 개발 PC 주소
  const host = Constants.expoConfig?.hostUri?.split(':')[0];
  if (host) return `http://${host}:8080`;

  return 'http://localhost:8080';
}

export const API_BASE_URL = resolve();