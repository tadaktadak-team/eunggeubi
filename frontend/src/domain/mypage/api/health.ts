import { api } from '../../../shared/api/client';
import { HealthProfile, HealthProfileInput, MedicationSearchItem } from '../types';

export function getHealthProfile() {
  return api.get<HealthProfile>('/api/health', { auth: true });
}

export function saveHealthProfile(body: HealthProfileInput) {
  return api.put<HealthProfile>('/api/health', body, { auth: true });
}

export function searchMedications(keyword: string) {
  return api.get<MedicationSearchItem[]>(
    `/api/health/medications/search?keyword=${encodeURIComponent(keyword)}`,
    { auth: true },
  );
}
