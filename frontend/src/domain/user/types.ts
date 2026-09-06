import { Gender } from '../auth/types';

// GET/POST /api/users/me 응답. 마이페이지의 MyInfo와 동일한 페이로드다.
export interface ProfileResponse {
  email: string;
  name: string;
  phone: string;
  birthDate: string; // 'yyyy-MM-dd'
  gender: Gender;
  address: string | null;
  socialOnly: boolean; // 비밀번호가 없는 소셜 전용 계정
}

export interface UpdateProfileRequest {
  name: string;
  phone: string;
  birthDate: string; // 'yyyy-MM-dd'
  gender: Gender;
  address?: string;
}
