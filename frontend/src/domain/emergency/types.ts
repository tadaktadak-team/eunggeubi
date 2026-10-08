export type Relationship = 'PARENT' | 'GRANDPARENT' | 'SIBLING' | 'OTHER';
// SENDING: 발송 접수(도착 여부는 응답 시점에 알 수 없다) / SKIPPED: 발송 횟수 제한으로 생략
export type SendStatus = 'SENDING' | 'SKIPPED';

// 문자를 생략한 이유. RECENTLY_SENT: 1분 안에 이미 보냄 / DAILY_LIMIT: 하루 발송 상한
export type SkipReason = 'RECENTLY_SENT' | 'DAILY_LIMIT';

export interface GuardianResult {
  name: string;
  phone: string;
  relationship: Relationship;
  status: SendStatus;
}