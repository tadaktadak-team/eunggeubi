import { api } from '../../../shared/api/client';

// 응급처치 안내 백엔드 연동

export interface FirstAidGuideDto {
  situation: string;
  title: string;
  steps: string[];
  ordered: boolean; // true면 steps를 순서대로 해야 하는 처치라 번호로, 아니면 점으로 보여준다.
  // 원인별로 처치가 다른 주제(예: 동물·벌레 물림)의 세부 상황. 비어 있지 않으면 steps는 비어 있다 -
  // 사용자가 하나를 고르면 그 값을 caseName으로 다시 조회한다.
  cases: string[];
}

// 상황(화상/코피/골절/기도막힘 등)에 해당하는 응급처치 단계를 조회한다.
export function getFirstAidGuide(situation: string, caseName?: string) {
  const query = caseName ? `?case=${encodeURIComponent(caseName)}` : '';
  return api.get<FirstAidGuideDto>(`/api/first-aid/guides/${encodeURIComponent(situation)}${query}`);
}

// 검색 후보 카드 한 장. topic은 getFirstAidGuide에 넘기는 원래 이름, 나머지는 카드에 보여주는 값이다
// (서버 first-aid-topics.txt에서 온다 - 표시 정보가 없는 주제는 shortName=topic, 나머지는 빈 문자열).
export interface FirstAidTopicDto {
  topic: string;
  shortName: string;
  category: string;
  description: string;
}

// 자유 텍스트로 상황 후보를 검색한다(분명하면 1개, 애매하면 최대 5개). 가이드는 같이 오지 않는다 -
// 사용자가 고른 카드의 topic으로 getFirstAidGuide를 부른다. 매칭이 없으면 candidates가 빈 배열이다.
export interface FirstAidSearchResult {
  candidates: FirstAidTopicDto[];
}

// "상황별로 찾기" 목록 - 고를 수 있는 주제 전체가 서버 목록 파일 순서(분류별로 모여 있다)대로 온다.
export function getFirstAidTopics() {
  return api.get<FirstAidSearchResult>('/api/first-aid/guides/topics');
}

export function searchFirstAidGuide(text: string) {
  return api.post<FirstAidSearchResult>('/api/first-aid/guides/search', { text });
}
