import { api } from '../../../shared/api/client';
import { HealthProfile } from '../types';

export function getHealthProfile() {
  return api.get<HealthProfile>('/api/health', { auth: true });
}

export function saveHealthProfile(body: HealthProfile) {
  return api.put<HealthProfile>('/api/health', body, { auth: true });
}