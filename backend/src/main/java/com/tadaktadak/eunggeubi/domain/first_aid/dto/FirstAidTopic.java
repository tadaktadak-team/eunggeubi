package com.tadaktadak.eunggeubi.domain.first_aid.dto;

// 응급처치 검색 후보 카드 한 장(first-aid-topics.txt 한 줄). topic은 가이드 조회에 쓰는 원래 이름(seed.csv의
// disease), shortName·category·description은 카드에 보여주는 값이다.
public record FirstAidTopic(String topic, String shortName, String category, String description) {

    // 목록 파일이 없어 전체 목록으로 검색할 때처럼 표시용 정보가 없는 주제 - 원래 이름만 보여준다.
    public static FirstAidTopic bare(String topic) {
        return new FirstAidTopic(topic, topic, "", "");
    }
}
