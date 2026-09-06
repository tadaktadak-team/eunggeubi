import { api } from '../../../shared/api/client';
import { MyInfo } from '../types';

// 마이페이지 요약 조회
export function getMyInfo() {
  return api.get<MyInfo>('/api/users/me', { auth: true });
}

export interface ChangePasswordResult {
  accessToken: string;
  refreshToken: string;
}

//비밀번호 변경 시 기존 토큰 삭제, 현 기기 로그인을 유지하기 위해서 토큰 두개를 쌍으로 보냄
export function changePassword(currentPassword: string, newPassword: string) {
  return api.put<ChangePasswordResult>(
    '/api/users/password',
    { currentPassword, newPassword },
    { auth: true },
  );
}
