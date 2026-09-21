package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟).
public record RawFirstAidGuide(String title, List<String> steps) {
}
