package com.tadaktadak.eunggeubi.domain.first_aid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidTopic;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawRelevanceCheck;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicCandidates;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicMatch;
import java.util.List;
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
        stubChatClientEntity(new RawTopicMatch(1, "직장탈출증"));

        assertThat(resolver.resolve("항문이 빠지는 느낌이에요")).contains("직장탈출증");
    }

    @Test
    void LLM이_범위를_벗어난_번호를_반환하면_환각으로_보고_빈값을_반환한다() {
        stubChatClientEntity(new RawTopicMatch(999, "없는이름"));

        assertThat(resolver.resolve("아무 증상")).isEmpty();
    }

    @Test
    void 관련_항목이_없다는_null_응답은_빈값으로_처리하고_재시도하지_않는다() {
        stubChatClientEntity(new RawTopicMatch(null, null));

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
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawTopicMatch.class)).thenReturn(new RawTopicMatch(1, "직장탈출증"));

        assertThat(resolver.resolve("항문이 빠지는 느낌이에요")).contains("직장탈출증");
    }

    // 매 요청 483개 질병명 목록 전체를 LLM에 다시 넣는 게 느리다는 지적(실측 확인)에 따라 query별로
    // 캐싱했다 - 같은 query로 두 번 요청해도 LLM 호출은 한 번만 나가야 한다.
    @Test
    void 같은_질의는_두번째_요청부터_LLM을_다시_부르지_않는다() {
        stubChatClientEntity(new RawTopicMatch(1, "직장탈출증"));

        Optional<String> first = resolver.resolve("항문이 빠지는 느낌이에요");
        Optional<String> second = resolver.resolve("항문이 빠지는 느낌이에요");

        assertThat(first).contains("직장탈출증");
        assertThat(second).isEqualTo(first);
        verify(chatClient, times(1)).prompt();
    }

    // 실패(매칭 없음)는 캐싱하지 않는다 - 두 번째 요청 때도 다시 LLM을 불러야 한다.
    @Test
    void 매칭에_실패한_질의는_캐싱하지_않고_매번_다시_시도한다() {
        stubChatClientEntity(new RawTopicMatch(null, null));

        resolver.resolve("주식 투자 방법");
        resolver.resolve("주식 투자 방법");

        verify(chatClient, times(2)).prompt();
    }

    @Test
    void 애매한_질의는_후보를_여러개_돌려주고_검증안되는_후보와_중복은_버린다() {
        stubChatClientEntity(new RawTopicMatch(null, null));
        when(chatClient.prompt().call().entity(RawTopicCandidates.class))
                .thenReturn(new RawTopicCandidates(List.of(
                        new RawTopicCandidates.Pick(2, "딸꾹질"),
                        new RawTopicCandidates.Pick(1, "직장탈출증"),
                        new RawTopicCandidates.Pick(2, "딸꾹질"),
                        new RawTopicCandidates.Pick(999, "없는이름"))));

        assertThat(resolver.resolveCandidates("아파요")).containsExactly("딸꾹질", "직장탈출증");
    }

    // 응급처치 검색 후보는 응급처치 목록(first-aid-topics.txt)에 있는 주제만 - 전체 목록엔 있어도 응급처치
    // 목록에 없으면(직장탈출증) 버린다. 목록이 없으면 전체 목록으로 검색한다(다른 테스트들이 그 경우).
    @Test
    void 응급처치_검색후보는_응급처치_목록에_있는_주제만_돌려준다() {
        ReflectionTestUtils.setField(resolver, "firstAidTopicsResource",
                new ClassPathResource("fixtures/first-aid-topics-sample.txt"));
        resolver.init();
        stubChatClientEntity(new RawTopicMatch(null, null));
        when(chatClient.prompt().call().entity(RawTopicCandidates.class))
                .thenReturn(new RawTopicCandidates(List.of(
                        new RawTopicCandidates.Pick(1, "직장탈출증"),
                        new RawTopicCandidates.Pick(1, "딸꾹질"))));

        assertThat(resolver.resolveCandidates("숨이 차요")).containsExactly("딸꾹질");
    }

    // 후보 카드 표시 정보는 목록 파일의 "[분류]"와 "주제 | 짧은 이름 | 설명"에서 온다. 목록에 없는 주제는 이름만.
    @Test
    void 응급처치_목록에서_카드_표시정보를_읽는다() {
        ReflectionTestUtils.setField(resolver, "firstAidTopicsResource",
                new ClassPathResource("fixtures/first-aid-topics-sample.txt"));
        resolver.init();

        assertThat(resolver.describe("딸꾹질"))
                .isEqualTo(new FirstAidTopic("딸꾹질", "딸꾹질 멈추기", "호흡", "딸꾹질이 멈추지 않을 때"));
        assertThat(resolver.describe("직장탈출증")).isEqualTo(FirstAidTopic.bare("직장탈출증"));
    }

    // 고른 후보를 짧게 다시 보여주고 관련 없다고 답한 건 뺀다("손"에 치아손상이 섞이던 문제).
    @Test
    void 관련성_확인에서_빠진_후보는_결과에서_뺀다() {
        stubChatClientEntity(new RawTopicMatch(null, null));
        when(chatClient.prompt().call().entity(RawTopicCandidates.class))
                .thenReturn(new RawTopicCandidates(List.of(
                        new RawTopicCandidates.Pick(1, "직장탈출증"),
                        new RawTopicCandidates.Pick(2, "딸꾹질"))));
        when(chatClient.prompt().call().entity(RawRelevanceCheck.class))
                .thenReturn(new RawRelevanceCheck(List.of(
                        new RawRelevanceCheck.Item(1, "항문", false),
                        new RawRelevanceCheck.Item(2, "횡격막", true))));

        assertThat(resolver.resolveCandidates("숨이 차요")).containsExactly("딸꾹질");
    }

    // 관련성 확인이 전부 지우면 0개 대신 1차 후보를 보여준다(응급 검색에선 0개가 더 나쁘다).
    @Test
    void 관련성_확인이_후보를_전부_지우면_1차_후보를_보여준다() {
        stubChatClientEntity(new RawTopicMatch(null, null));
        when(chatClient.prompt().call().entity(RawTopicCandidates.class))
                .thenReturn(new RawTopicCandidates(List.of(new RawTopicCandidates.Pick(2, "딸꾹질"))));
        when(chatClient.prompt().call().entity(RawRelevanceCheck.class))
                .thenReturn(new RawRelevanceCheck(List.of(new RawRelevanceCheck.Item(1, "횡격막", false))));

        assertThat(resolver.resolveCandidates("아파요")).containsExactly("딸꾹질");
    }

    // 483개짜리 목록에서 LLM이 옆 번호를 잘못 적는 경우(화상 38 대신 황달 39)가 실측으로 잦았다 -
    // 이름이 목록에 그대로 있으면 번호가 틀려도 이름을 믿는다.
    @Test
    void 번호가_틀려도_이름이_목록에_있으면_이름을_따른다() {
        stubChatClientEntity(new RawTopicMatch(1, "딸꾹질"));

        assertThat(resolver.resolve("딸꾹질이 안 멈춰요")).contains("딸꾹질");
    }

    // 이름을 줄여 써도 그 번호 자리의 이름이 그걸 포함하면 받아준다(예: "기도폐쇄" → 심폐소생술(...기도폐쇄의 처치)).
    @Test
    void 줄여쓴_이름은_번호_자리의_이름이_포함하면_받아준다() {
        stubChatClientEntity(new RawTopicMatch(1, "탈출증"));

        assertThat(resolver.resolve("항문이 빠지는 느낌이에요")).contains("직장탈출증");
    }

    @Test
    void 번호와_이름이_서로_안맞고_이름도_목록에_없으면_버린다() {
        stubChatClientEntity(new RawTopicMatch(2, "황달"));

        assertThat(resolver.resolve("눈이 노래요")).isEmpty();
    }

    // 후보 칩을 눌러 들어오면 목록 이름이 그대로 온다 - LLM을 다시 부르지 않아야 한다.
    @Test
    void 목록에_있는_이름_그대로면_LLM을_부르지_않는다() {
        assertThat(resolver.resolve("딸꾹질")).contains("딸꾹질");
        assertThat(resolver.resolveCandidates("딸꾹질")).containsExactly("딸꾹질");
        verify(chatClient, times(0)).prompt();
    }

    @SuppressWarnings("unchecked")
    private void stubChatClientEntity(RawTopicMatch value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawTopicMatch.class)).thenReturn(value);
    }
}
