import { api } from '../../../shared/api/client';
import { ConsultationDetail, ConsultationSummary } from '../types';

//상담 이력 목록 (세션의 첫 질문만 최신순)
export function getConsultations() {
  return api.get<ConsultationSummary[]>('/api/users/me/consultations', { auth: true });
}

//상담 한 건의 전체 대화
export function getConsultationDetail(sessionId: string) {
  return api.get<ConsultationDetail>(`/api/users/me/consultations/${sessionId}`, { auth: true });
}