import { api } from '../../../shared/api/client';

// 백엔드 InteractionResponse DTO 구조와 동일한 타입 선언
export interface InteractionResponse {
  itemSeqA: string;
  itemNameA: string;
  itemSeqB: string;
  itemNameB: string;
  reasons: string[];   // 같은 쌍의 금기 사유가 여러 개일 수 있음
}

/*
선택한 약들(itemSeq 목록) 중 서로 병용금기인 쌍을 조회
백엔드 엔드포인트: POST /api/drugs/interactions/check
결과가 빈 배열이면 병용 가능하다는 뜻
*/
export const checkInteractions = async (itemSeqs: string[]): Promise<InteractionResponse[]> => {
  return api.post<InteractionResponse[]>('/api/drugs/interactions/check', { itemSeqs });
};
