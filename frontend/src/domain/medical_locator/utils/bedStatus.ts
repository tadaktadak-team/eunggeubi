import { colors } from '../../../shared/theme/theme';
import { EmergencyBed } from '../types/emergencyBed';

export type BedStatus = { label: string; color: string };

const AMBER = '#D97706';

// 응급실 API는 수용 인원을 넘으면 잔여 병상을 음수로 내려준다. 숫자를 그대로 보여주지 않고 상태로 바꾼다
export function getBedStatus(bed: Pick<EmergencyBed, 'availableBeds' | 'congestion'>): BedStatus | null {
  const { availableBeds, congestion } = bed;
  if (availableBeds == null && congestion == null) return null;
  if ((availableBeds != null && availableBeds <= 0) || (congestion != null && congestion >= 100)) {
    return { label: '포화', color: colors.danger };
  }
  if (congestion != null && congestion >= 70) return { label: '혼잡', color: AMBER };
  if (congestion != null && congestion >= 40) return { label: '보통', color: colors.textSub };
  return { label: '여유', color: colors.success };
}

export function formatBeds(availableBeds: number | null) {
  if (availableBeds == null) return '병상 정보 없음';
  return availableBeds <= 0 ? '여유 병상 없음' : `여유 병상 ${availableBeds}`;
}

// "20261002033035" → "03:30"
export function formatUpdatedAt(updatedAt: string | null | undefined) {
  if (!updatedAt || updatedAt.length < 12) return null;
  return `${updatedAt.slice(8, 10)}:${updatedAt.slice(10, 12)}`;
}
