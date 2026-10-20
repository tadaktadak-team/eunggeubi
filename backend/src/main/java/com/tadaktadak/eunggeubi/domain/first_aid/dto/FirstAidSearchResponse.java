package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 응급처치 검색 결과 - 상황 후보 카드 목록(분명하면 1개, 애매하면 최대 5개). 프론트가 사용자에게 고르게 한 뒤
// 고른 카드의 topic으로 GET /{situation}을 다시 부른다. 매칭이 없으면 빈 배열.
public record FirstAidSearchResponse(List<FirstAidTopic> candidates) {
}
