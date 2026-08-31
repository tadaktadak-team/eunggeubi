package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void 길이가_짧으면_안_쪼갠다() {
        String text = "짧은 문장입니다.";

        List<String> result = TextChunker.split(text, 3000);

        assertThat(result).containsExactly(text);
    }

    @Test
    void 길이_제한을_넘으면_여러_조각으로_쪼갠다() {
        // 실제 사례(자주하는 질문 9023자, 참고문헌 14592자)를 흉내낸 긴 텍스트
        String sentence = "이것은 테스트용 문장입니다. ";
        String longText = sentence.repeat(500); // 약 7500자

        List<String> result = TextChunker.split(longText, 3000);

        assertThat(result.size()).isGreaterThan(1);
        for (String piece : result) {
            assertThat(piece.length()).isLessThanOrEqualTo(3000);
        }
        // 쪼개도 원문 내용이 유실되면 안 된다 (공백 trim 차이는 있을 수 있어 이어붙여서 비교)
        assertThat(String.join("", result).replaceAll("\\s", ""))
                .isEqualTo(longText.replaceAll("\\s", ""));
    }

    @Test
    void 문장_경계가_없어도_강제로_잘라서_무한루프에_안_빠진다() {
        String noBoundary = "가".repeat(10000); // 마침표/줄바꿈이 전혀 없는 극단적인 경우

        List<String> result = TextChunker.split(noBoundary, 3000);

        assertThat(result.size()).isGreaterThanOrEqualTo(4); // 10000/3000 -> 최소 4조각
        for (String piece : result) {
            assertThat(piece.length()).isLessThanOrEqualTo(3000);
        }
    }
}
