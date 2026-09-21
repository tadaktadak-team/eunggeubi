package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 프론트 FirstAidGuideDto(situation, title, steps)와 필드를 그대로 맞춘 응답이다.
public record FirstAidGuideResponse(String situation, String title, List<String> steps) {

    public static FirstAidGuideResponse of(String situation, RawFirstAidGuide raw) {
        return new FirstAidGuideResponse(situation, raw.title(), raw.steps());
    }
}
