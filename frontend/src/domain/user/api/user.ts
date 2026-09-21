import { api } from '../../../shared/api/client';
import { ProfileResponse, UpdateProfileRequest } from '../types';

export function getProfile() {
  return api.get<ProfileResponse>('/api/users/me', { auth: true });
}

export function updateProfile(payload: UpdateProfileRequest) {
  return api.post<ProfileResponse>('/api/users/me', payload, { auth: true });
}

export function withdraw(password: string) {
  return api.post<void>('/api/users/withdraw', { password }, { auth: true });
}
