// AI 증상 상담 채팅 메시지
export type ChatMessage =
  | UserChatMessage
  | ReferenceChatMessage
  | ChecklistChatMessage
  | AnswerChatMessage
  | RegeneratedAnswerChatMessage;

export interface UserChatMessage {
  id: string;
  type: 'user';
  text: string;
}

// 백엔드 RAG 응답(POST /api/ai-consultations) 그대로 담는 메시지.
// 문장(segment) 단위로 근거 자료 번호(sourceIndexes)를 갖고 있어서, 문장별로 인용 배지를 붙일 수 있다.
// consultationId는 이 답변이 저장된 상담 id - 뒤이어 자동으로 붙는 체크리스트 생성/제출에 쓰인다.
// 네트워크 에러를 로컬에서 흉내낸 메시지에는 실제 상담 id가 없어서 없을 수 있다.
// disclaimer는 RegeneratedAnswerChatMessage와 같은 자리 - 예전엔 segments 안에 문장으로 섞여왔다.
export interface AnswerChatMessage {
  id: string;
  type: 'answer';
  consultationId?: number;
  segments: AnswerSegment[];
  sources: AnswerSource[];
  disclaimer?: string;
}

// 체크리스트까지 반영한 증상 원문이 health_info 컬렉션에 실제 현장 응급처치 콘텐츠가 있는 상황과
// 관련 있을 때만(4개로 한정하지 않음 - FirstAidGuideService.search 참고) 백엔드가 채워준다 - 최초
// 답변이 아니라 체크리스트 제출 후 재생성된 답변(RegeneratedAnswerChatMessage)에만 붙는다.
export interface RelatedAidGuide {
  situation: FirstAidSituation;
  title: string;
}

// 체크리스트 결과를 반영해 다시 생성한 답변(POST /{id}/regenerate). 문장별 인용이 아니라
// 메시지 전체에 대한 flat 출처 목록 - 백엔드 스키마가 그렇게 되어 있다(AiConsultationController 참고).
export interface RegeneratedAnswerChatMessage {
  id: string;
  type: 'regenerated';
  message: string;
  sources: RegeneratedSource[];
  disclaimer: string;
  relatedAidGuide?: RelatedAidGuide | null;
}

export interface RegeneratedSource {
  referenceSourceId: number;
  title: string;
  urlOrOrg: string;
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
  consultationId: number; // 제출(submit)·재생성(regenerate) 호출에 그대로 쓰인다
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

// 응급처치 안내 화면의 "빠른 선택" 칩 - 백엔드는 이 4개로 제한하지 않고 health_info 컬렉션에 있는
// 어떤 상황이든 검색/조회할 수 있어서(FirstAidGuideService), situation은 고정 유니온이 아니라
// 일반 문자열이다. 이 목록은 화면에 기본으로 보여줄 추천 칩일 뿐이다.
export const FIRST_AID_SITUATIONS = ['화상', '코피', '골절', '기도막힘'] as const;
export type FirstAidSituation = string;

// AI상담 탭 내부 네비게이션
export type AiConsultationStackParamList = {
  SymptomHome: undefined;
  SymptomChat: { initialMessage?: string } | undefined;
  FirstAidGuide: { situation?: FirstAidSituation } | undefined;
};
