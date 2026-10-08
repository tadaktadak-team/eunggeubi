import { api } from '../../../shared/api/client';
import { GuardianResult, SkipReason } from '../types';

export interface SendAlertResponse {
  sentAt: string;
  message: string;
  guardians: GuardianResult[];
  skipReason: SkipReason | null;
}

export function sendEmergencyAlert(latitude: number, longitude: number, address?: string) {
  return api.post<SendAlertResponse>(
    '/api/emergency/alert',
    { latitude, longitude, address },
    { auth: true },
  );
}