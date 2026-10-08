package com.tadaktadak.eunggeubi.domain.drug.service;

// 사용자 입력을 LIKE 패턴에 그대로 넣으면 %, _ 가 리터럴이 아니라 와일드카드로 해석돼서
// "%" 하나만 검색해도 전체가 나오거나, "95%"처럼 실제 약 이름에 들어있는 글자를 찾을 때
// 의도보다 넓게 매칭된다. 이스케이프 문자는 쿼리의 ESCAPE '!'와 짝이 맞아야 한다
// (백슬래시는 MySQL/Hibernate 양쪽에서 이스케이프가 겹쳐 오작동하기 쉬워 피함).
final class LikeEscaper {

    private LikeEscaper() {
    }

    static String escape(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
