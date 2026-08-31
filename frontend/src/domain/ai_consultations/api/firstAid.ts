import { api } from '../../../shared/api/client';

// 응급처치 안내 백엔드 연동

export interface FirstAidGuideDto {
  situation: string;
  title: string;
  steps: string[];
}

// 상황(화상/코피/골절/기도막힘 등)에 해당하는 응급처치 단계를 조회한다.
export function getFirstAidGuide(situation: string) {
  return api.get<FirstAidGuideDto>(`/api/first-aid/guides/${encodeURIComponent(situation)}`);
}

// 자유 텍스트로 상황을 검색한다. 매칭되는 상황이 없으면 서버가 null을 내려준다.
export function searchFirstAidGuide(text: string) {
  return api.post<FirstAidGuideDto | null>('/api/first-aid/guides/search', { text });
}
