package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟) - 재생성용.
// 출처 메타데이터는 LLM이 지어낼 수 있으므로 인용 번호(citedSourceIndexes)만 받고, 실제 출처 정보는
// 서버가 검색 결과에서 채운다 (AiConsultationService의 RawAnswer와 같은 원칙).
public record RawRegeneratedAnswer(String message, List<Integer> citedSourceIndexes) {
}
