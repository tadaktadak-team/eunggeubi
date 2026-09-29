export type Relationship = 'PARENT' | 'GRANDPARENT' | 'SIBLING' | 'OTHER';
export type SendStatus = 'SENDING' | 'SENT' | 'FAILED';

export interface GuardianResult {
  name: string;
  phone: string;
  relationship: Relationship;
  status: SendStatus;
}