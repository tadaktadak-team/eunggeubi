package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

// 체크리스트 응답은 이미 submit 단계에서 저장돼 있으므로, 재생성 요청은 소유권 확인용 guestCode만 필요하다.
public record RegenerateRequest(String guestCode) {
}
