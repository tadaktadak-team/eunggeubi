package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// 문장(segment) 단위로 답변을 쪼개고, 각 문장이 어떤 출처를 근거로 했는지 sourceIndexes로 표시한다.
// sourceIndexes가 비어있으면 인사말/119 안내/디스클레이머처럼 특정 자료에 근거하지 않은 문장이다.
// sources는 답변 문장 중 하나라도 실제로 인용한 자료만 담는다 - 검색은 됐지만 답변에 안 쓰인 문서를
// "출처"로 보여주면 프론트에서 오해를 살 수 있어서 제외한다.
//
// consultationId는 이 AI 응답이 저장된 ai_consultations 행의 id다 - 프론트는 이후 체크리스트 생성/제출,
// 재생성 호출 시 이 id를 그대로 path에 실어 보내야 한다. sessionId/guestCode도 마찬가지로 이후 요청에
// 그대로 실어 보내야 하는 값인데, guestCode는 이번에 "새로 발급된 경우"에만 값이 채워진다 (기존 세션을
// 이어가는 요청이면 클라이언트가 이미 갖고 있으므로 null로 내려간다).
// disclaimer는 RegenerateResponse와 같은 자리·같은 문구를 쓴다 - 원래는 answer 안에 문장으로 섞여
// 있었는데, 재생성 응답과 계약이 달라서 프론트가 두 가지를 따로 처리해야 했다.
public record ConsultationResponse(
        List<AnswerSegment> answer,
        List<Source> sources,
        Long consultationId,
        String sessionId,
        String guestCode,
        String disclaimer
) {

    public record AnswerSegment(String text, List<Integer> sourceIndexes) {
    }

    public record Source(int index, String disease, String section, String sourceName, String cntntsSn) {
    }
}
