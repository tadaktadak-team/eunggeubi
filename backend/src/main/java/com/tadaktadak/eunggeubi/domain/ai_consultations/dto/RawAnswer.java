package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import java.util.List;

// LLM이 생성하는 원본 구조화 출력(ChatClient.entity() 타겟).
// 출처 메타데이터(disease/section 등)는 LLM이 지어낼 수 있으므로 여기엔 참고자료 번호(sourceIndexes)만
// 담게 하고, 실제 메타데이터는 서버가 검색 결과(Document)에서 직접 채운다.
public record RawAnswer(List<RawSegment> segments) {

    public record RawSegment(String text, List<Integer> sourceIndexes) {
    }
}
