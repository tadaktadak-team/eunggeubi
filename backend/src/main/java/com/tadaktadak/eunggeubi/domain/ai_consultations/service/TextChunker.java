package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.ArrayList;
import java.util.List;

// 섹션 본문이 너무 길면(실제로 "자주하는 질문" "참고문헌" 등에서 9000~14000자짜리가 나왔음)
// 그대로 임베딩에 넣었을 때 OpenAI 입력 토큰 한도(8191)를 넘어 통째로 실패한다.
// 이를 막기 위해 일정 길이(maxChars) 이하로 쪼갠다. 가능하면 문장/줄바꿈 경계에서 자르고,
// 마땅한 경계가 없으면 그냥 maxChars에서 강제로 자른다.
final class TextChunker {

    private TextChunker() {
    }

    static List<String> split(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return List.of(text);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            if (end < text.length()) {
                int breakPoint = lastBreakPoint(text, start, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    // [start, end) 구간의 뒤쪽 절반 안에서 문장/줄바꿈이 끝나는 지점을 찾는다.
    // 뒤쪽 절반으로 제한하는 이유: 너무 앞에서 끊으면 마지막 조각이 지나치게 작아짐.
    private static int lastBreakPoint(String text, int start, int end) {
        int searchFrom = start + (end - start) / 2;
        for (int i = end - 1; i > searchFrom; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        return end;
    }
}
