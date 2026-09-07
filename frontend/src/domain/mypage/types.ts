export type Relationship = 'PARENT' | 'GRANDPARENT' | 'SIBLING' | 'OTHER';

export interface Guardian {
  id: number;
  name: string;
  relationship: Relationship;
  phone: string;
  notifyEnabled: boolean;
}

export const RELATIONSHIP_LABEL: Record<Relationship, string> = {
  PARENT: '부모',
  GRANDPARENT: '조부모',
  SIBLING: '형제자매',
  OTHER: '기타',
};

export const RELATIONSHIP_OPTIONS: Relationship[] = ['PARENT', 'GRANDPARENT', 'SIBLING', 'OTHER'];

export interface HealthProfile {
  bloodType: string | null;
  diseases: string[];
  medications: string[];
}

export const BLOOD_TYPES = ['A', 'B', 'O', 'AB'];

export type Gender = 'MALE' | 'FEMALE' | 'NONE';

// GET /api/users/me 응답
export interface MyInfo {
  email: string;
  name: string;
  phone: string;
  birthDate: string; // "1990-01-01"
  gender: Gender;
  address: string | null;
  socialOnly: boolean; //소셜
}

// GET /api/users/me/consultations 응답 항목
export interface ConsultationSummary {
  sessionId: string;
  firstQuestion: string;
  createdAt: string; // 타임존 없는 서버 로컬시간
}

export type SenderType = 'USER' | 'AI';

export interface ConsultationMessage {
  id: number;
  senderType: SenderType;
  content: string;
  regenerated: boolean;
  createdAt: string;
  checkedItems: string[];
  checklistAnswered: boolean;
}

// GET /api/users/me/consultations/{sessionId} 응답 (MY04_HIST02)
export interface ConsultationDetail {
  sessionId: string;
  messages: ConsultationMessage[];
}

export type MyPageStackParamList = {
  MyPageHome: undefined;
  Guardian: undefined;
  GuardianForm: { guardian?: Guardian } | undefined; // 수정/등록
  Health: undefined;
  ChangePassword: undefined;
  AccountEdit: undefined;
  Withdraw: undefined;
  ConsultationHistory: undefined;
  ConsultationDetail: { sessionId: string; title: string };
};