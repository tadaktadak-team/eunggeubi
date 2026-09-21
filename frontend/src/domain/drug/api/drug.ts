import { api } from '../../../shared/api/client';

// 백엔드 DrugInfoResponse DTO 구조와 동일한 타입 선언
export interface DrugInfoResponse {
  itemSeq: string;       // 품목일련번호
  name: string;          // 약품명
  shape?: string;        // 모양
  color?: string;        // 색상
  imprint?: string;      // 각인(식별문자)
  efficacy?: string;     // 효능/효과
  useInfo?: string;      // 용법/용량
  caution?: string;      // 주의사항
  drugType?: string;     // 약품구분
  itemImage?: string;    // 알약 이미지 URL
}

/*
1. 약품명 키워드 검색 API
백엔드 엔드포인트: GET /api/drugs/search?keyword={keyword}
*/
export const searchDrugsByName = async (keyword: string): Promise<DrugInfoResponse[]> => {
  return api.get<DrugInfoResponse[]>(
    `/api/drugs/search?keyword=${encodeURIComponent(keyword)}`,
  );
};

/*
2. 약품 상세 정보 조회 API
백엔드 엔드포인트: GET /api/drugs/{itemSeq}
*/
export const getDrugDetail = async (itemSeq: string): Promise<DrugInfoResponse> => {
  return api.get<DrugInfoResponse>(`/api/drugs/${itemSeq}`);
};