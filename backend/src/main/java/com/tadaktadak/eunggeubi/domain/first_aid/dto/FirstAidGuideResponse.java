package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 프론트 FirstAidGuideDto(situation, title, steps, ordered)와 필드를 그대로 맞춘 응답이다.
public record FirstAidGuideResponse(String situation, String title, List<String> steps, boolean ordered) {

    // ordered는 RawStepOrder 판단 결과. 한 단계뿐이면 순서라는 게 없어 true여도 번호를 붙이지 않는다.
    // null(판단 실패)도 번호 없이 둔다 - 번호는 순서가 꼭 필요하다고 판단된 처치에만 붙인다.
    public static FirstAidGuideResponse of(String situation, RawFirstAidGuide raw, Boolean ordered) {
        boolean numbered = raw.steps().size() > 1 && Boolean.TRUE.equals(ordered);
        return new FirstAidGuideResponse(situation, raw.title(), raw.steps(), numbered);
    }
}
