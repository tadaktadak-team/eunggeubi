import { api } from '../../../shared/api/client';
import { MyInfo } from '../types';

// 마이페이지 요약 조회 (MY01_INFO01)
export function getMyInfo() {
  return api.get<MyInfo>('/api/users/me', { auth: true });
}

// 성공 시 204 No Content (본문 없음) — client.ts가 undefined로 처리해준다
export function changePassword(currentPassword: string, newPassword: string) {
  return api.put<void>('/api/users/password', { currentPassword, newPassword }, { auth: true });
}
