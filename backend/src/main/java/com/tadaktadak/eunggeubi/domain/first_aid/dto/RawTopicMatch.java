package com.tadaktadak.eunggeubi.domain.first_aid.dto;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟) - 상황-질병명 매칭용.
// 이름을 직접 베껴 쓰게 하면(문자열 그대로) LLM이 미묘하게 줄이거나 바꿔 써서(예: "기도폐쇄"로
// 답했는데 실제 목록엔 "심폐소생술(이물질에 의한 기도폐쇄의 처치)") 검증에서 걸러지는 경우가 실측으로
// 잦았다 - 그래서 번호로 고르게 한다. 관련 있는 항목이 없으면 matchedIndex를 null로 응답하도록
// 프롬프트에서 지시한다.
public record RawTopicMatch(Integer matchedIndex) {
}
