import { api } from '../../../shared/api/client';
import {
  AnswerChatMessage,
  AnswerSegment,
  AnswerSource,
  ChecklistChatMessage,
  ChecklistItem,
  RegeneratedAnswerChatMessage,
  RegeneratedSource,
} from '../types';

interface ConsultationResponseDto {
  answer: AnswerSegment[];
  sources: AnswerSource[];
  consultationId: number;
  sessionId: string;
  guestCode: string | null; // 새 게스트 세션일 때만 값이 옴 (기존 세션 이어가는 요청이면 null)
}

interface ChecklistDto {
  checklistId: number;
  title: string;
  items: string[];
  status: string;
}

interface RegenerateResponseDto {
  message: string;
  isDiagnosis: boolean;
  sources: RegeneratedSource[];
  disclaimer: string;
}

let idSeq = 0;
function nextId() {
  idSeq += 1;
  return `msg-${Date.now()}-${idSeq}`;
}

export interface SymptomAdviceResult {
  answerMessage: AnswerChatMessage;
  checklistMessage: ChecklistChatMessage | null;
  sessionId: string;
  guestCode: string | null; // 이번 호출로 새로 발급됐으면 값이 있고, 기존 걸 그대로 썼으면 null
}

// 증상 텍스트를 보내면 1차 답변을 받고, 답변에 실제 출처가 있으면(=참고할 근거가 있으면) 이어서
// 체크리스트도 자동으로 생성해서 같이 돌려준다. 체크리스트 생성이 실패해도 1차 답변은 이미 받았으니
// 조용히 넘어간다(사용자에게 1차 답변만이라도 보여주는 게 낫다).
export async function requestSymptomAdvice(
  text: string,
  sessionId?: string,
  guestCode?: string,
): Promise<SymptomAdviceResult> {
  const response = await api.post<ConsultationResponseDto>(
    '/api/ai-consultations',
    { query: text, sessionId, guestCode },
    { auth: true }, // accessToken 자동 첨부 (게스트는 토큰 없이도 통과 - 백엔드가 permitAll)
  );

  const answerMessage: AnswerChatMessage = {
    id: nextId(),
    type: 'answer',
    consultationId: response.consultationId,
    segments: response.answer,
    sources: response.sources,
  };

  const effectiveGuestCode = response.guestCode ?? guestCode ?? undefined;
  let checklistMessage: ChecklistChatMessage | null = null;
  if (response.sources.length > 0) {
    try {
      checklistMessage = await generateChecklist(response.consultationId, effectiveGuestCode);
    } catch (e) {
      console.error('체크리스트 생성 실패', e);
    }
  }

  return {
    answerMessage,
    checklistMessage,
    sessionId: response.sessionId,
    guestCode: response.guestCode,
  };
}

async function generateChecklist(consultationId: number, guestCode?: string): Promise<ChecklistChatMessage> {
  const checklist = await api.post<ChecklistDto>(
    `/api/ai-consultations/${consultationId}/checklist`,
    { guestCode },
    { auth: true },
  );

  const items: ChecklistItem[] = checklist.items.map((label) => ({ id: nextId(), label, checked: false }));
  return { id: nextId(), type: 'checklist', consultationId, title: checklist.title, items, answered: false };
}

export function submitChecklistAnswers(consultationId: number, selectedItems: string[], guestCode?: string) {
  return api.post<{ checklistResponseId: number; status: string }>(
    `/api/ai-consultations/${consultationId}/checklist/submit`,
    { selectedItems, guestCode },
    { auth: true },
  );
}

export async function regenerateAnswer(
  consultationId: number,
  guestCode?: string,
): Promise<RegeneratedAnswerChatMessage> {
  const response = await api.post<RegenerateResponseDto>(
    `/api/ai-consultations/${consultationId}/regenerate`,
    { guestCode },
    { auth: true },
  );

  return {
    id: nextId(),
    type: 'regenerated',
    message: response.message,
    sources: response.sources,
    disclaimer: response.disclaimer,
  };
}

export function nextChatMessageId() {
  return nextId();
}
