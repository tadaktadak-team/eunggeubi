export type Relationship = 'PARENT' | 'GRANDPARENT' | 'SIBLING' | 'OTHER';

export interface Guardian {
  id: number;
  name: string;
  relationship: Relationship;
  phone: string;
}

export const RELATIONSHIP_LABEL: Record<Relationship, string> = {
  PARENT: '부모',
  GRANDPARENT: '조부모',
  SIBLING: '형제자매',
  OTHER: '기타',
};

export const RELATIONSHIP_OPTIONS: Relationship[] = ['PARENT', 'GRANDPARENT', 'SIBLING', 'OTHER'];

export type MyPageStackParamList = {
  MyPageHome: undefined;
  Guardian: undefined;
  GuardianForm: { guardian?: Guardian } | undefined; // 있으면 수정, 없으면 등록
};