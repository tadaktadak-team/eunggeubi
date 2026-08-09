// AI 증상 상담 채팅 메시지
export type ChatMessage = UserChatMessage | ReferenceChatMessage | ChecklistChatMessage;

export interface UserChatMessage {
  id: string;
  type: 'user';
  text: string;
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
