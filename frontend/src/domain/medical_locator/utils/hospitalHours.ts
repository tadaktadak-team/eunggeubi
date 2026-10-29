import { HospitalDetail } from '../types/hospitalDetail';

export const NO_INFO_MESSAGE = '아직 등록된 정보가 없어요. 병원에 직접 문의해주세요';

export const DAY_LABELS: {
  key: keyof HospitalDetail;
  endKey: keyof HospitalDetail;
  label: string;
}[] = [
  { key: 'mondayStart', endKey: 'mondayEnd', label: '월요일' },
  { key: 'tuesdayStart', endKey: 'tuesdayEnd', label: '화요일' },
  { key: 'wednesdayStart', endKey: 'wednesdayEnd', label: '수요일' },
  { key: 'thursdayStart', endKey: 'thursdayEnd', label: '목요일' },
  { key: 'fridayStart', endKey: 'fridayEnd', label: '금요일' },
  { key: 'saturdayStart', endKey: 'saturdayEnd', label: '토요일' },
];

export function formatTime(value: string | null | undefined) {
  if (!value || value.length !== 4) return null;
  return `${value.slice(0, 2)}:${value.slice(2, 4)}`;
}

// 평일(월~금) 진료시간이 모두 같으면 "평일 09:00–18:00"으로 요약, 아니면 null
export function getWeekdayHoursSummary(detail: HospitalDetail | null) {
  if (!detail) return null;
  const weekdays = DAY_LABELS.slice(0, 5).map(({ key, endKey }) => {
    const start = formatTime(detail[key] as string | null);
    const end = formatTime(detail[endKey] as string | null);
    return start && end ? `${start}–${end}` : null;
  });
  if (weekdays[0] && weekdays.every((w) => w === weekdays[0])) {
    return `평일 ${weekdays[0]}`;
  }
  return null;
}

// 평일 요약이 없을 때 쓰는 "오늘" 진료 정보. 정보가 없으면 null (일요일은 일요일 안내 문구가 있을 때만)
export function getTodayHoursText(detail: HospitalDetail | null) {
  if (!detail) return null;
  const day = new Date().getDay(); // 0=일 ... 6=토
  if (day === 0) {
    return detail.closedOnSunday ? `오늘(일) ${detail.closedOnSunday}` : null;
  }
  const { key, endKey } = DAY_LABELS[day - 1];
  const start = formatTime(detail[key] as string | null);
  const end = formatTime(detail[endKey] as string | null);
  return start && end ? `오늘 ${start}–${end}` : null;
}
