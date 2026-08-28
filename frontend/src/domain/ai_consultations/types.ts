// AI 증상 상담 채팅 메시지
export type ChatMessage = UserChatMessage | ReferenceChatMessage | ChecklistChatMessage | AnswerChatMessage;

export interface UserChatMessage {
  id: string;
  type: 'user';
  text: string;
}

// 백엔드 RAG 응답(POST /api/ai-consultations) 그대로 담는 메시지.
// 문장(segment) 단위로 근거 자료 번호(sourceIndexes)를 갖고 있어서, 문장별로 인용 배지를 붙일 수 있다.
export interface AnswerChatMessage {
  id: string;
  type: 'answer';
  segments: AnswerSegment[];
  sources: AnswerSource[];
}

export interface AnswerSegment {
  text: string;
  sourceIndexes: number[]; // AnswerSource.index를 가리킴. 비어있으면 특정 자료에 근거하지 않은 문장.
}

export interface AnswerSource {
  index: number;
  disease: string;
  section: string;
  sourceName: string;
  cntntsSn: string;
}

export interface ReferenceChatMessage {
  id: string;
  type: 'reference';
  title: string;
  body: string;
  source: string;
}

export interface ChecklistChatMessage {
  id: string;
  type: 'checklist';
  title: string;
  items: ChecklistItem[];
  answered: boolean; // 답변하기를 눌렀는지 여부 (누르면 체크박스 비활성화)
}

export interface ChecklistItem {
  id: string;
  label: string;
  checked: boolean;
}

// 홈 화면 빠른 증상 선택 칩
export const QUICK_SYMPTOMS = ['두통', '복통', '발열', '어지럼'] as const;
export type QuickSymptom = (typeof QUICK_SYMPTOMS)[number];

// 응급처치 안내 화면의 상황 선택 칩
export const FIRST_AID_SITUATIONS = ['화상', '코피', '골절', '기도막힘'] as const;
export type FirstAidSituation = (typeof FIRST_AID_SITUATIONS)[number];

// AI상담 탭 내부 네비게이션
export type AiConsultationStackParamList = {
  SymptomHome: undefined;
  SymptomChat: { initialMessage?: string } | undefined;
  FirstAidGuide: { situation?: FirstAidSituation } | undefined;
};
