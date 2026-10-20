package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 프론트 FirstAidGuideDto(situation, title, steps, ordered, cases)와 필드를 그대로 맞춘 응답이다.
// cases가 비어 있지 않으면 아직 세부 상황을 안 고른 것이라 steps는 비어 있다 - 프론트가 하나를 고르게 한 뒤
// ?case=로 다시 부른다(FirstAidTopic.cases 참고).
public record FirstAidGuideResponse(String situation, String title, List<String> steps, boolean ordered,
                                    List<String> cases) {

    // title은 화면 제목(FirstAidGuideService.titleFor). ordered는 RawStepOrder 판단 결과 - 한 단계뿐이면
    // 순서라는 게 없어 true여도 번호를 붙이지 않는다. null(판단 실패)도 번호 없이 둔다 - 번호는 순서가 꼭
    // 필요하다고 판단된 처치에만 붙인다.
    public static FirstAidGuideResponse of(String situation, String title, RawFirstAidGuide raw, Boolean ordered) {
        boolean numbered = raw.steps().size() > 1 && Boolean.TRUE.equals(ordered);
        return new FirstAidGuideResponse(situation, title, raw.steps(), numbered, List.of());
    }

    // 세부 상황 고르기 - 가이드는 아직 만들지 않는다.
    public static FirstAidGuideResponse chooseCase(String situation, String title, List<String> cases) {
        return new FirstAidGuideResponse(situation, title, List.of(), false, cases);
    }
}
