import { api } from '../../../shared/api/client';
import { AnswerChatMessage, AnswerSegment, AnswerSource } from '../types';

interface ConsultationResponseDto {
  answer: AnswerSegment[];
  sources: AnswerSource[];
}

let idSeq = 0;
function nextId() {
  idSeq += 1;
  return `msg-${Date.now()}-${idSeq}`;
}

// 증상 텍스트를 보내면 문장별 인용이 달린 답변 카드 하나를 응답으로 준다.
export async function requestSymptomAdvice(text: string): Promise<AnswerChatMessage[]> {
  const response = await api.post<ConsultationResponseDto>(
    '/api/ai-consultations',
    { query: text },
    { auth: true }, // accessToken 자동 첨부
  );

  return [
    {
      id: nextId(),
      type: 'answer',
      segments: response.answer,
      sources: response.sources,
    },
  ];
}

export function nextChatMessageId() {
  return nextId();
}
