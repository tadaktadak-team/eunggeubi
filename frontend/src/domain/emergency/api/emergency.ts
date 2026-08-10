import { api } from '../../../shared/api/client';
import { GuardianResult } from '../types';

export interface SendAlertResponse {
  sentAt: string;
  message: string;
  guardians: GuardianResult[];
}

export function sendEmergencyAlert(latitude: number, longitude: number) {
  return api.post<SendAlertResponse>(
    '/api/emergency/alert',
    { latitude, longitude },
    { auth: true }, // accessToken 자동 첨부
  );
}