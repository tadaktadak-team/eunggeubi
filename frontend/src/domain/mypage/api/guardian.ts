import { api } from '../../../shared/api/client';
import { Guardian, Relationship } from '../types';

export interface GuardianRequest {
  name: string;
  phone: string;
  relationship: Relationship;
  notifyEnabled?: boolean; // 등록 시 생략하면 서버에서 기본 true
}

export function getGuardians() {
  return api.get<Guardian[]>('/api/guardians', { auth: true });
}

export function addGuardian(body: GuardianRequest) {
  return api.post<Guardian>('/api/guardians', body, { auth: true });
}

export function updateGuardian(id: number, body: GuardianRequest) {
  return api.put<Guardian>(`/api/guardians/${id}`, body, { auth: true });
}

export function deleteGuardian(id: number) {
  return api.delete<void>(`/api/guardians/${id}`, { auth: true });
}
