package com.tadaktadak.eunggeubi.domain.first_aid.dto;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟) - 상황-질병명 매칭용.
// 번호와 이름을 둘 다 받아 서로 검증한다(HealthTopicResolver.verify). 이름만 받으면 LLM이 미묘하게
// 줄여 쓰고(예: "기도폐쇄" → 실제 목록엔 "심폐소생술(이물질에 의한 기도폐쇄의 처치)"), 번호만 받으면
// 483개짜리 목록에서 옆 번호를 잘못 적는 경우(예: 화상 38 대신 황달 39)가 실측으로 잦았다.
// 관련 있는 항목이 없으면 둘 다 null로 응답하도록 프롬프트에서 지시한다.
public record RawTopicMatch(Integer matchedIndex, String matchedName) {
}
