package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// 문장(segment) 단위로 답변을 쪼개고, 각 문장이 어떤 출처를 근거로 했는지 sourceIndexes로 표시한다.
// sourceIndexes가 비어있으면 인사말/119 안내/디스클레이머처럼 특정 자료에 근거하지 않은 문장이다.
// sources는 답변 문장 중 하나라도 실제로 인용한 자료만 담는다 - 검색은 됐지만 답변에 안 쓰인 문서를
// "출처"로 보여주면 프론트에서 오해를 살 수 있어서 제외한다.
public record ConsultationResponse(List<AnswerSegment> answer, List<Source> sources) {

    public record AnswerSegment(String text, List<Integer> sourceIndexes) {
    }

    public record Source(int index, String disease, String section, String sourceName, String cntntsSn) {
    }
}
