import { Gender, UserStatus } from '../auth/types';

export interface ProfileResponse {
  userId: number;
  email: string;
  name: string;
  phone: string;
  birthDate: string; // 'yyyy-MM-dd'
  gender: Gender;
  address: string | null;
  status: UserStatus;
}

export interface UpdateProfileRequest {
  name: string;
  phone: string;
  birthDate: string; // 'yyyy-MM-dd'
  gender: Gender;
  address?: string;
}
