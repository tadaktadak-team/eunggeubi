package com.tadaktadak.eunggeubi.domain.first_aid.dto;

import java.util.List;

// 응급처치 검색 후보마다 관련 여부를 판단한 결과 - HealthTopicPrompts.RELEVANCE_CHECK_PROMPT.
// 번호만 고르게 했을 땐 "손"에 치아외상(치아손상)을 계속 남겼다(실측). 후보마다 실제로 생기는 부위(bodyPart)를
// 먼저 적게 하면 "치아"라고 쓴 뒤 스스로 빼서, 판단 근거를 같이 받는다.
public record RawRelevanceCheck(List<Item> items) {

    public record Item(Integer index, String bodyPart, Boolean relevant) {
    }
}
