import * as Linking from 'expo-linking';
import * as WebBrowser from 'expo-web-browser';

import { api } from '../../../shared/api/client';
import { Gender, LoginResponse } from '../types';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

type SocialProvider = 'naver' | 'kakao'| 'google';

// 소셜 로그인 결과
export type SocialLoginResult =
  | { type: 'login'; userId: number; accessToken: string; refreshToken: string }
  | { type: 'signup'; ticket: string; needInfo: boolean } // 신규 → 약관(+정보) 입력 필요
  | { type: 'cancel' };

// 공통: 인앱 브라우저로 백엔드 authorize 를 열고 복귀 URL을 파싱
async function startSocialLogin(provider: SocialProvider): Promise<SocialLoginResult> {
  const appRedirect = Linking.createURL('oauth');
  const startUrl =
    `${API_BASE_URL}/api/auth/social/${provider}/authorize` +
    `?appRedirect=${encodeURIComponent(appRedirect)}`;

    
  const result = await WebBrowser.openAuthSessionAsync(startUrl, appRedirect);
  if (result.type !== 'success' || !result.url) {
    return { type: 'cancel' };
  }

  const { queryParams } = Linking.parse(result.url);

  if (queryParams?.needConsent === 'true') {
    return {
      type: 'signup',
      ticket: String(queryParams?.ticket ?? ''),
      needInfo: queryParams?.needInfo === 'true', // true면 전화/생년월일/성별도 받아야 함(카카오 등)
    };
  }

  return {
    type: 'login',
    userId: Number(queryParams?.userId),
    accessToken: String(queryParams?.accessToken ?? ''),
    refreshToken: String(queryParams?.refreshToken ?? ''),
  };
}

export const startNaverLogin = () => startSocialLogin('naver');
export const startKakaoLogin = () => startSocialLogin('kakao');
export const startGoogleLogin = () => startSocialLogin('google');

// 신규 소셜 가입 완료. 부족한 정보(전화/생년월일/성별)는 extra 로 함께 보낸다(카카오).
export interface SocialExtraInfo {
  phone?: string;
  birthDate?: string; // 'yyyy-MM-dd'
  gender?: Gender;
}

export function completeSocialSignup(
  ticket: string,
  agreements: { agreeService: boolean; agreePrivacy: boolean; agreeSensitiveInfo: boolean },
  extra?: SocialExtraInfo,
) {
  return api.post<LoginResponse>('/api/auth/social/complete', { ticket, ...agreements, ...extra });
}