package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 응급처치 검색 후보 카드 한 장(first-aid-topics.txt 한 줄). topic은 가이드 조회에 쓰는 원래 이름(seed.csv의
// disease), shortName·category·description은 카드에 보여주는 값이다. cases는 원인별로 처치가 다른 주제의
// 세부 상황(예: 개·고양이에게 물림/뱀에게 물림/벌에 쏘임) - 비어 있지 않으면 화면이 하나를 고르게 한다.
public record FirstAidTopic(String topic, String shortName, String category, String description, List<String> cases) {

    public FirstAidTopic(String topic, String shortName, String category, String description) {
        this(topic, shortName, category, description, List.of());
    }

    // 목록 파일이 없어 전체 목록으로 검색할 때처럼 표시용 정보가 없는 주제 - 원래 이름만 보여준다.
    public static FirstAidTopic bare(String topic) {
        return new FirstAidTopic(topic, topic, "", "");
    }

    // 화면에 보이는 이름 - 짧은 이름 뒤 괄호에 원래 이름(예: "베인 상처 (열상)"). 둘이 같으면 한 번만.
    // 프론트 FirstAidGuideScreen의 candidateLabel과 같은 규칙이다.
    public String label() {
        return shortName.equals(topic) ? topic : "%s (%s)".formatted(shortName, topic);
    }
}
