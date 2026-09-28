import { api } from '../../../shared/api/client';
import { GuardianResult } from '../types';

export interface SendAlertResponse {
  sentAt: string;
  message: string;
  guardians: GuardianResult[];
}

export function sendEmergencyAlert(latitude: number, longitude: number, address?: string) {
  return api.post<SendAlertResponse>(
    '/api/emergency/alert',
    { latitude, longitude, address },
    { auth: true },
  );
}