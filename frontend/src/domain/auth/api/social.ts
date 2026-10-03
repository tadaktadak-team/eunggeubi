import * as Linking from 'expo-linking';
import * as WebBrowser from 'expo-web-browser';
import { API_BASE_URL } from '../../../shared/api/config';
import { api } from '../../../shared/api/client';
import { Gender, LoginResponse } from '../types';


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
  // 서버가 되돌려준 오류: 이미 가입된 이메일 (소셜 자동연동은 허용하지 않는다)
  if (queryParams?.error === 'email_taken') {
    throw new Error('이미 가입된 이메일입니다. 기존에 사용하던 방법으로 로그인해주세요.');
  }


  if (queryParams?.needConsent === 'true') {
    return {
      type: 'signup',
      ticket: String(queryParams?.ticket ?? ''),
      needInfo: queryParams?.needInfo === 'true', // true면 전화/생년월일/성별도 받아야 함(카카오 등)
    };
  }

  // 기존 회원: URL 에는 1회용 티켓만 실려 온다. 토큰은 본문으로 따로 받아온다.
  const loginTicket = String(queryParams?.loginTicket ?? '');
  if (!loginTicket) {
    throw new Error('로그인 응답이 올바르지 않습니다. 다시 시도해주세요.');
  }
  const tokens = await api.post<LoginResponse>('/api/auth/social/exchange', {
    ticket: loginTicket,
  });
  return {
    type: 'login',
    userId: tokens.userId,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
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