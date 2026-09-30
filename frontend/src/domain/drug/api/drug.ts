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

export interface DrugSearchPageResponse {
  items: DrugInfoResponse[];
  pageNo: number;
  numOfRows: number;
  totalCount: number;
}

/*
1. 약품명 키워드 검색 API (페이지네이션)
백엔드 엔드포인트: GET /api/drugs/search?keyword={keyword}&pageNo={pageNo}&numOfRows={numOfRows}
*/
export const searchDrugsByName = async (
  keyword: string,
  pageNo: number = 1,
  numOfRows: number = 10,
): Promise<DrugSearchPageResponse> => {
  return api.get<DrugSearchPageResponse>(
    `/api/drugs/search?keyword=${encodeURIComponent(keyword)}&pageNo=${pageNo}&numOfRows=${numOfRows}`,
  );
};

/*
로컬 DB 검색 API (페이지네이션 없음, 서버에서 상위 30건으로 제한)
백엔드 엔드포인트: GET /api/drugs/local-search?keyword={keyword}
e약은요(OTC 위주, 좁음) 대신 우리 DB(낱알식별로 적재된 25,000여 건, 전문의약품 포함)에서
검색한다. 상호작용 체크 화면에서 "약 검색해서 추가"할 때 쓴다.
*/
export const searchLocalDrugs = async (keyword: string): Promise<DrugInfoResponse[]> => {
  return api.get<DrugInfoResponse[]>(`/api/drugs/local-search?keyword=${encodeURIComponent(keyword)}`);
};

/*
2. 약품 상세 정보 조회 API
백엔드 엔드포인트: GET /api/drugs/{itemSeq}
*/
export const getDrugDetail = async (itemSeq: string): Promise<DrugInfoResponse> => {
  return api.get<DrugInfoResponse>(`/api/drugs/${itemSeq}`);
};