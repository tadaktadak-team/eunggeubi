import { HealthProfile } from '../types';

// TODO: 백엔드 연동 시 GET /api/health, PUT /api/health 로 교체
export async function getHealthProfile(): Promise<HealthProfile> {
  await new Promise((r) => setTimeout(r, 300));
  return { bloodType: null, diseases: [], allergies: [], medications: [] };
}

export async function saveHealthProfile(body: HealthProfile): Promise<HealthProfile> {
  await new Promise((r) => setTimeout(r, 300));
  return body;
}