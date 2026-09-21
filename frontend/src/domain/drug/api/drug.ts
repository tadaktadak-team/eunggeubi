import { api } from '../../../shared/api/client';

// 백엔드 DrugInfoResponse DTO 구조와 동일한 타입 선언
export interface DrugInfoResponse {
  itemSeq: string;              // 품목일련번호
  itemName: string;             // 제품명
  entpName?: string;            // 업체명
  efcyQesitm?: string;          // 효능/효과
  useMethodQesitm?: string;     // 용법/용량
  atpnQesitm?: string;          // 주의사항
  itemImage?: string;           // 약물 이미지 URL
  depositMethodQesitm?: string; // 보관법
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