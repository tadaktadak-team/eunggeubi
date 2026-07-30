import { api } from '../../../shared/api/client';
import {
  FindEmailResponse,
  LoginResponse,
  Purpose,
  SignupRequest,
  SignupResponse,
  GuardianRequest,
  GuardianConsentResponse,
  ConsentStatusResponse,
} from '../types';

export function signup(payload: SignupRequest) {
  return api.post<SignupResponse>('/api/auth/signup', payload);
}

export function login(email: string, password: string) {
  return api.post<LoginResponse>('/api/auth/login', { email, password });
}

export function sendPhoneCode(phone: string, purpose: Purpose) {
  return api.post<void>('/api/auth/phone/send', { phone, purpose });
}

export function verifyPhoneCode(phone: string, purpose: Purpose, code: string) {
  return api.post<void>('/api/auth/phone/verify', { phone, purpose, code });
}

export function findEmail(name: string, phone: string) {
  return api.post<FindEmailResponse>('/api/auth/find-email', { name, phone });
}

export function resetPassword(email: string, phone: string, newPassword: string) {
  return api.post<void>('/api/auth/reset-password', { email, phone, newPassword });
}

export function reissue(refreshToken: string) {
  return api.post<LoginResponse>('/api/auth/reissue', { refreshToken });
}

export function logout(refreshToken: string) {
  return api.post<void>('/api/auth/logout', { refreshToken });
}

export function requestGuardianConsent(payload: GuardianRequest) {
  return api.post<GuardianConsentResponse>('/api/auth/guardian/request', payload);
}

export function getGuardianConsentStatus(userId: number) {
  return api.get<ConsentStatusResponse>(`/api/auth/guardian/status?userId=${userId}`);
}