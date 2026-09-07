import { api } from '../../../shared/api/client';
import { Gender, MyInfo } from '../types';

// 마이페이지 요약 조회
export function getMyInfo() {
  return api.get<MyInfo>('/api/users/me', { auth: true });
}

export interface UpdateMyInfoBody {
  name: string;
  phone: string;
  birthDate: string;
  gender: Gender;
  address: string | null;
}

// 회원 정보 수정 (이메일은 변경 불가)
export function updateMyInfo(body: UpdateMyInfoBody) {
  return api.put<MyInfo>('/api/users/me', body, { auth: true });
}

// 소셜 전용 계정은 비밀번호가 없어 생략할 수 있다
export function withdraw(password?: string) {
  return api.post<void>('/api/users/me/withdraw', { password }, { auth: true });
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
