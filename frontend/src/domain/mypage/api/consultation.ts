import { api } from '../../../shared/api/client';
import { ConsultationDetail, ConsultationSummary } from '../types';

//상담 이력 목록 (세션의 첫 질문만 최신순)
export function getConsultations() {
  return api.get<ConsultationSummary[]>('/api/users/me/consultations', { auth: true });
}

//이 기기의 비회원 상담 기록(guestCode)을 내 계정으로 가져오기 - claimed = 옮겨진 메시지 수
export function claimGuestConsultations(guestCode: string) {
  return api.post<{ claimed: number }>('/api/users/me/consultations/claim-guest', { guestCode }, { auth: true });
}

//상담 한 건의 전체 대화
export function getConsultationDetail(sessionId: string) {
  return api.get<ConsultationDetail>(`/api/users/me/consultations/${sessionId}`, { auth: true });
}