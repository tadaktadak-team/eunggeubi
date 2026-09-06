import { api } from '../../../shared/api/client';
import { MyInfo } from '../types';

// 마이페이지 요약 조회 (MY01_INFO01)
export function getMyInfo() {
  return api.get<MyInfo>('/api/users/me', { auth: true });
}

export interface ChangePasswordResult {
  accessToken: string;
  refreshToken: string;
}

// 서버가 기존 refresh 토큰을 전부 폐기하므로, 현재 기기가 계속 쓸 새 토큰 쌍이 함께 내려온다.
// 받은 값을 저장하지 않으면 access 토큰이 만료되는 순간(최대 1시간) 로그아웃된다.
export function changePassword(currentPassword: string, newPassword: string) {
  return api.put<ChangePasswordResult>(
    '/api/users/password',
    { currentPassword, newPassword },
    { auth: true },
  );
}
