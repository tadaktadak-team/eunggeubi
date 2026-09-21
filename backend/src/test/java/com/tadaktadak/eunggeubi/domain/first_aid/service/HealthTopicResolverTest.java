package com.tadaktadak.eunggeubi.domain.first_aid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicMatch;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class HealthTopicResolverTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final HealthTopicResolver resolver = new HealthTopicResolver(chatClient);

    // fixtures/seed-sample.csv: 1.직장탈출증 / 2.딸꾹질 / 3.실패케이스 (HealthInfoIndexingRunnerTest와 공유).
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resolver, "seedResource", new ClassPathResource("fixtures/seed-sample.csv"));
        resolver.init();
    }

    @Test
    void 목록_번호에_해당하는_이름을_돌려준다() {
        stubChatClientEntity(new RawTopicMatch(1));

        assertThat(resolver.resolve("항문이 빠지는 느낌이에요")).contains("직장탈출증");
    }

    @Test
    void LLM이_범위를_벗어난_번호를_반환하면_환각으로_보고_빈값을_반환한다() {
        stubChatClientEntity(new RawTopicMatch(999));

        assertThat(resolver.resolve("아무 증상")).isEmpty();
    }

    @Test
    void 관련_항목이_없다는_null_응답은_빈값으로_처리하고_재시도하지_않는다() {
        stubChatClientEntity(new RawTopicMatch(null));

        assertThat(resolver.resolve("주식 투자 방법")).isEmpty();
        // matchedIndex=null은 정상적인 "매칭 없음" 최종 응답이라 재시도 대상이 아니다 - 1번만 호출돼야 한다.
        verify(chatClient, times(1)).prompt();
    }

    @Test
    void LLM_호출이_실패하면_예외를_던지지_않고_빈값을_반환한다() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("LLM 호출 실패"));

        assertThat(resolver.resolve("아무 증상")).isEmpty();
    }

    // 같은 이유로 FirstAidGuideServiceTest에도 있는 재시도 회귀 테스트 - 예외는 한 번의 일시적 오류일
    // 수 있으니, 첫 시도가 실패해도 재시도해서 살려내야 한다.
    @Test
    void LLM_호출이_한번_실패해도_재시도해서_성공한다() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt())
                .thenThrow(new RuntimeException("일시적 오류"))
                .thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawTopicMatch.class)).thenReturn(new RawTopicMatch(1));

        assertThat(resolver.resolve("항문이 빠지는 느낌이에요")).contains("직장탈출증");
    }

    // 매 요청 483개 질병명 목록 전체를 LLM에 다시 넣는 게 느리다는 지적(실측 확인)에 따라 query별로
    // 캐싱했다 - 같은 query로 두 번 요청해도 LLM 호출은 한 번만 나가야 한다.
    @Test
    void 같은_질의는_두번째_요청부터_LLM을_다시_부르지_않는다() {
        stubChatClientEntity(new RawTopicMatch(1));

        Optional<String> first = resolver.resolve("항문이 빠지는 느낌이에요");
        Optional<String> second = resolver.resolve("항문이 빠지는 느낌이에요");

        assertThat(first).contains("직장탈출증");
        assertThat(second).isEqualTo(first);
        verify(chatClient, times(1)).prompt();
    }

    // 실패(매칭 없음)는 캐싱하지 않는다 - 두 번째 요청 때도 다시 LLM을 불러야 한다.
    @Test
    void 매칭에_실패한_질의는_캐싱하지_않고_매번_다시_시도한다() {
        stubChatClientEntity(new RawTopicMatch(null));

        resolver.resolve("주식 투자 방법");
        resolver.resolve("주식 투자 방법");

        verify(chatClient, times(2)).prompt();
    }

    @SuppressWarnings("unchecked")
    private void stubChatClientEntity(RawTopicMatch value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawTopicMatch.class)).thenReturn(value);
    }
}
