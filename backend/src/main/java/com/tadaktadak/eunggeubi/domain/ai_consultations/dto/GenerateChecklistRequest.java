package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

// 게스트(비로그인)는 guestCode로 본인 상담인지 확인한다. 로그인 사용자는 JWT로 확인되므로 비워도 된다.
public record GenerateChecklistRequest(String guestCode) {
}
