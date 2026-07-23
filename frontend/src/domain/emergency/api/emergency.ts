import { GuardianResult } from '../types';

export interface SendAlertResponse {
  sentAt: string;
  guardians: GuardianResult[];
}

// TODO: 백엔드 연동 시 POST /api/emergency/alert 호출로 교체 (지금은 mock)
export async function sendEmergencyAlert(
  latitude: number,
  longitude: number,
): Promise<SendAlertResponse> {
  await new Promise((resolve) => setTimeout(resolve, 500));

  return {
    sentAt: new Date().toISOString(),
    guardians: [
      { name: '김보호', phone: '010-1111-2222', relationship: 'PARENT', status: 'SENT' },
      { name: '이가족', phone: '010-3333-4444', relationship: 'SIBLING', status: 'SENT' },
    ],
  };
}
