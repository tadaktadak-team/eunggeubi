package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟) - RAG 검색어 재작성용.
public record RawSearchQuery(String searchTerms) {
}
