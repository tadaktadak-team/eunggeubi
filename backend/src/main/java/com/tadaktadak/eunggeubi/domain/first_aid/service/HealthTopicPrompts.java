package com.tadaktadak.eunggeubi.domain.first_aid.service;

// HealthTopicResolver가 쓰는 시스템 프롬프트.
final class HealthTopicPrompts {

    private HealthTopicPrompts() {
    }

    static final String RESOLVE_PROMPT = """
            너는 사용자가 설명한 상황과 가장 가까운 질병/상황명을 아래 [목록]에서 골라주는 도우미다.
            [목록]은 "번호. 이름" 형태로 되어 있다.

            [반드시 지킬 규칙]
            1. matchedIndex에는 고른 항목의 번호만 정수로 담는다. 이름을 담지 않는다.
            2. [상황]과 표현이 다르더라도 의미가 통하는 항목이 [목록]에 있으면 그것을 고른다
               (예: "기도막힘"이라는 표현은 [목록]에 "기도폐쇄"나 "질식"을 다루는 항목이 있다면
               그 항목을 가리킬 수 있다).
            3. 정말 관련 있는 항목이 없으면 matchedIndex를 null로 둔다. 억지로 아무거나 고르지 않는다.
            4. 같은 계열 항목이 여러 개 있으면(예: 화상/일광화상) [상황]에 가장 가깝고 일반적인 것
               번호 하나만 고른다.
            """;
}
