import { api } from '../../../shared/api/client';

// 성공 시 204 No Content (본문 없음) — client.ts가 undefined로 처리해준다
export function changePassword(currentPassword: string, newPassword: string) {
  return api.put<void>('/api/users/password', { currentPassword, newPassword }, { auth: true });
}