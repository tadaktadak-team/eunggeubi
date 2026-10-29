package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// RawTopicMatch의 후보 여러 개 버전 - 응급처치 검색창에서 검색어가 애매할 때 사용자가 고를 후보를
// 최대 5개까지 받는다. 번호와 이름을 같이 받는 이유는 RawTopicMatch와 같다.
public record RawTopicCandidates(List<Pick> matches) {

    public record Pick(Integer index, String name) {
    }
}
