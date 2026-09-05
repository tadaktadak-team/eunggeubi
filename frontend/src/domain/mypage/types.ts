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

// GET /api/users/me 응답 (MY01_INFO01)
export interface MyInfo {
  email: string;
  name: string;
  phone: string;
  birthDate: string; // "1990-01-01"
  gender: Gender;
  address: string | null;
  socialOnly: boolean; // 비밀번호가 없는 소셜 전용 계정
}

export type MyPageStackParamList = {
  MyPageHome: undefined;
  Guardian: undefined;
  GuardianForm: { guardian?: Guardian } | undefined; // 있으면 수정, 없으면 등록
  Health: undefined;
  ChangePassword: undefined;
};