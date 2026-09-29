import { api } from '../../../shared/api/client';

// 백엔드 PillSearchResponse DTO 구조와 동일한 타입 선언
export interface PillSearchResponse {
  itemSeq: string;       // 품목일련번호
  itemName: string;      // 약품명
  entpName?: string;     // 업체명(제조사)
  itemImage?: string;    // 알약 이미지 URL
  drugShape?: string;    // 모양
  colorClass?: string;   // 색상
  printFront?: string;   // 각인 앞
  printBack?: string;    // 각인 뒤
  etcOtcName?: string;   // 전문의약품/일반의약품 구분
}

export interface PillSearchParams {
  drugShape?: string;
  colorClass?: string;
  printFront?: string;
}

export const searchPills = async (params: PillSearchParams): Promise<PillSearchResponse[]> => {
  const query = new URLSearchParams();
  if (params.drugShape) query.set('drugShape', params.drugShape);
  if (params.colorClass) query.set('colorClass', params.colorClass);
  if (params.printFront) query.set('printFront', params.printFront);

  return api.get<PillSearchResponse[]>(`/api/drugs/pills/identification?${query.toString()}`);
};