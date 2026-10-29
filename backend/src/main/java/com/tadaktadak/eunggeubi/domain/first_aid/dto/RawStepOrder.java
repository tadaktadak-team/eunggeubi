package com.tadaktadak.eunggeubi.domain.first_aid.dto;

// 만들어진 응급처치 steps를 순서대로 해야 하는지 판단한 LLM 출력(FirstAidPrompts.STEP_ORDER_PROMPT).
// 가이드 생성과 한 번에 묻으면 거의 모든 가이드를 true로 답해서(실측 - 18개 중 17개, "골절은 false"
// 예시를 줘도 true) 완성된 steps만 보여주고 따로 묻는다.
public record RawStepOrder(Boolean ordered) {
}
