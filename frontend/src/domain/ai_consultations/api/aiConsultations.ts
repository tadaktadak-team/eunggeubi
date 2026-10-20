import { api } from '../../../shared/api/client';
import {
  AnswerChatMessage,
  AnswerSegment,
  AnswerSource,
  ChecklistChatMessage,
  ChecklistItem,
  RegeneratedAnswerChatMessage,
  RegeneratedSource,
  RelatedAidGuide,
} from '../types';

interface ConsultationResponseDto {
  answer: AnswerSegment[];
  sources: AnswerSource[];
  consultationId: number;
  sessionId: string;
  guestCode: string | null; // 새 게스트 세션일 때만 값이 옴 (기존 세션 이어가는 요청이면 null)
  disclaimer: string;
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
  relatedAidGuide: RelatedAidGuide | null;
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

// 증상 텍스트를 보내면 1차 답변을 스트리밍으로 받고, 답변에 실제 출처가 있으면(=참고할 근거가 있으면)
// 이어서 체크리스트도 자동으로 생성해서 같이 돌려준다. 체크리스트 생성이 실패해도 1차 답변은 이미
// 받았으니 조용히 넘어간다(사용자에게 1차 답변만이라도 보여주는 게 낫다).
// onAnswer는 답변이 바뀔 때마다 같은 id의 메시지로 불린다 - 받는 중(완성된 문장까지, 출처 없음)과
// 최종 답변(출처 포함) 모두. 최종 답변은 받는 중 문장과 다를 수 있다(119 안내 추가·안전 문구로 교체).
export async function requestSymptomAdvice(
  text: string,
  sessionId: string | undefined,
  guestCode: string | undefined,
  onAnswer: (message: AnswerChatMessage) => void,
): Promise<SymptomAdviceResult> {
  const id = nextId();
  const response = await streamUntilDone<ConsultationResponseDto>(
    '/api/ai-consultations/stream',
    { query: text, sessionId, guestCode },
    (segments) => onAnswer({ id, type: 'answer', segments: segments as AnswerSegment[], sources: [] }),
  );

  const answerMessage: AnswerChatMessage = {
    id,
    type: 'answer',
    consultationId: response.consultationId,
    segments: response.answer,
    sources: response.sources,
    disclaimer: response.disclaimer,
  };
  onAnswer(answerMessage);

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

// 체크리스트를 반영한 답변을 스트리밍으로 받는다. onAnswer는 requestSymptomAdvice와 같다 - 받는 중(끝난
// 문장까지, 출처 없음)과 최종 답변(출처·응급처치 안내 포함) 모두 같은 id의 메시지로 불린다.
export async function regenerateAnswer(
  consultationId: number,
  guestCode: string | undefined,
  onAnswer: (message: RegeneratedAnswerChatMessage) => void,
): Promise<RegeneratedAnswerChatMessage> {
  const id = nextId();
  const response = await streamUntilDone<RegenerateResponseDto>(
    `/api/ai-consultations/${consultationId}/regenerate/stream`,
    { guestCode },
    (partial) =>
      onAnswer({ id, type: 'regenerated', message: (partial as { message: string }).message, sources: [], disclaimer: '' }),
  );

  const answer: RegeneratedAnswerChatMessage = {
    id,
    type: 'regenerated',
    message: response.message,
    sources: response.sources,
    disclaimer: response.disclaimer,
    relatedAidGuide: response.relatedAidGuide,
  };
  onAnswer(answer);
  return answer;
}

// 서버 스트림(AiConsultationController.stream)을 받아 partial은 onPartial로 넘기고 done 데이터를 돌려준다.
// error 이벤트는 일반 요청 실패와 같은 모양(message, code)의 Error로 던진다.
async function streamUntilDone<T>(path: string, body: unknown, onPartial: (data: unknown) => void): Promise<T> {
  let result: T | undefined;
  await api.postStream(
    path,
    body,
    (event, data) => {
      if (event === 'partial') onPartial(data);
      else if (event === 'done') result = data as T;
      else if (event === 'error') {
        const { message, code } = data as { message: string; code?: string };
        throw Object.assign(new Error(message), { code });
      }
    },
    { auth: true }, // accessToken 자동 첨부 (게스트는 토큰 없이도 통과 - 백엔드가 permitAll)
  );
  if (!result) throw new Error('답변을 받는 중 연결이 끊겼어요. 다시 시도해주세요.');
  return result;
}

export function nextChatMessageId() {
  return nextId();
}
