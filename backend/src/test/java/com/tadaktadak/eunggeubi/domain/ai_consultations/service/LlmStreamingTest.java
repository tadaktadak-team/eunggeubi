package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmStreamingTest {

    @Test
    void 닫힌_값만_뽑는다() {
        String partial = "{\"segments\":[{\"text\":\"물을 \\\"충분히\\\" 드세요.\",\"sourceIndexes\":[1]},"
                + "{\"text\":\"열이 계속되면 병원에";

        assertThat(LlmStreaming.closedStrings(partial, "text")).containsExactly("물을 \"충분히\" 드세요.");
        assertThat(LlmStreaming.closedStrings(partial + " 가세요.\"}]}", "text"))
                .containsExactly("물을 \"충분히\" 드세요.", "열이 계속되면 병원에 가세요.");
    }

    @Test
    void 안_닫힌_값에서는_끝난_문장까지만_뽑는다() {
        String partial = "{\"message\":\"체온이 38.5도 이상이면 해열제를 드세요. 물을 충분히";

        assertThat(LlmStreaming.completedSentences(partial, "message")).isEqualTo("체온이 38.5도 이상이면 해열제를 드세요.");
        // 문장부호 뒤에 공백이 오기 전엔 문장이 끝난 걸로 보지 않는다("38."에서 자르지 않게).
        assertThat(LlmStreaming.completedSentences("{\"message\":\"체온이 38.", "message")).isEmpty();
        // 덜 온 이스케이프는 떼고 디코딩한다.
        assertThat(LlmStreaming.completedSentences(partial + " 드세요. \\u", "message"))
                .isEqualTo("체온이 38.5도 이상이면 해열제를 드세요. 물을 충분히 드세요.");
        // 값이 닫히면 마지막 문장도 끝난 것이다.
        assertThat(LlmStreaming.completedSentences(partial + " 드세요\",\"citedSourceIndexes\":[1]}", "message"))
                .isEqualTo("체온이 38.5도 이상이면 해열제를 드세요. 물을 충분히 드세요");
    }
}
