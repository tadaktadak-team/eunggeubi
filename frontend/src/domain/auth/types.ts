export type Gender = 'MALE' | 'FEMALE' | 'NONE';
export type Purpose = 'SIGNUP' | 'FIND_ID' | 'FIND_PW';
export type UserStatus = 'ACTIVE' | 'PENDING' | 'WITHDRAWN';

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
  birthDate: string; // 'yyyy-MM-dd'
  gender: Gender;
  address?: string;
  agreeService: boolean;
  agreePrivacy: boolean;
  agreeSensitiveInfo: boolean;
}

export interface SignupResponse {
  userId: number;
  status: UserStatus;
  guardianConsentRequired: boolean;
}

export interface LoginResponse {
  userId: number;
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface FindEmailResponse {
  email: string; // 마스킹된 이메일
}