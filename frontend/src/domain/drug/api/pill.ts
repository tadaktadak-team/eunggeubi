import { api } from '../../../shared/api/client';

// 백엔드 PillSearchResponse DTO 구조와 동일한 타입 선언
export interface PillSearchResponse {
  itemSeq: string;       // 품목일련번호
  itemName: string;      // 약품명
  entpName?: string;     // 업체명(제조사)
  itemImage?: string;    // 알약 이미지 URL
  drugShape?: string;    // 모양
  colorClass?: string;   // 색상
  imprint?: string;      // 각인(식별문자)
  etcOtcName?: string;   // 전문의약품/일반의약품 구분
}

export interface PillSearchParams {
  drugShape?: string;
  colorClass?: string;
  imprint?: string;
  pageNo?: number;
  numOfRows?: number;
}

export interface PillSearchPageResponse {
  items: PillSearchResponse[];
  pageNo: number;
  numOfRows: number;
  totalCount: number;
}

export const searchPills = async (params: PillSearchParams): Promise<PillSearchPageResponse> => {
  const query = new URLSearchParams();
  if (params.drugShape) query.set('drugShape', params.drugShape);
  if (params.colorClass) query.set('colorClass', params.colorClass);
  if (params.imprint) query.set('imprint', params.imprint);
  if (params.pageNo) query.set('pageNo', String(params.pageNo));
  if (params.numOfRows) query.set('numOfRows', String(params.numOfRows));

  return api.get<PillSearchPageResponse>(`/api/drugs/pills/identification?${query.toString()}`);
};
