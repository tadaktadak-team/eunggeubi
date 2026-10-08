import { api } from '../../../shared/api/client';

// 백엔드 InteractionResponse DTO 구조와 동일한 타입 선언
export interface InteractionResponse {
  itemSeqA: string;
  itemNameA: string;
  itemSeqB: string;
  itemNameB: string;
  // 같은 쌍의 금기 사유가 여러 개일 수 있다. 원본 데이터에 사유가 아예 없는 쌍도 있어(전체 쌍의
  // 약 0.4%) 빈 배열이 올 수 있고, 구버전 서버 응답 등에 대비해 null/undefined도 허용한다.
  reasons?: string[] | null;
}

/*
선택한 약들(itemSeq 목록) 중 서로 병용금기인 쌍을 조회
백엔드 엔드포인트: POST /api/drugs/interactions/check
결과가 빈 배열이면 병용 가능하다는 뜻
*/
export const checkInteractions = async (itemSeqs: string[]): Promise<InteractionResponse[]> => {
  return api.post<InteractionResponse[]>('/api/drugs/interactions/check', { itemSeqs });
};
