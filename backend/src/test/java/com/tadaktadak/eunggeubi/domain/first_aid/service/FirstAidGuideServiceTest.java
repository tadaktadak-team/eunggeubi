package com.tadaktadak.eunggeubi.domain.first_aid.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.service.RagRetrievalService;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawFirstAidGuide;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

class FirstAidGuideServiceTest {

    private final RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final HealthTopicResolver healthTopicResolver = mock(HealthTopicResolver.class);
    private final FirstAidGuideService service =
            new FirstAidGuideService(ragRetrievalService, chatClient, healthTopicResolver);

    @Test
    void 상황명_매칭은_HealthTopicResolver에_위임한다() {
        when(healthTopicResolver.resolve("손에 화상을 입었어요")).thenReturn(Optional.of("화상"));
        when(healthTopicResolver.resolve("배가 아파요")).thenReturn(Optional.empty());

        assertThat(service.matchKnownSituation("손에 화상을 입었어요")).contains("화상");
        assertThat(service.matchKnownSituation("배가 아파요")).isEmpty();
    }

    @Test
    void 검색결과가_있으면_LLM이_생성한_단계로_가이드를_만든다() {
        when(healthTopicResolver.resolve("손에 화상을 입었어요")).thenReturn(Optional.of("화상"));
        Document doc = new Document("화상 관련 내용", Map.of("disease", "화상"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (화상) ...");
        stubChatClientEntity(new RawFirstAidGuide("화상 응급처치", List.of("찬물로 식힙니다.")));

        var result = service.search("손에 화상을 입었어요");

        assertThat(result).isPresent();
        assertThat(result.get().situation()).isEqualTo("화상");
        assertThat(result.get().steps()).containsExactly("찬물로 식힙니다.");
    }

    @Test
    void 가까운_상황명을_못_찾으면_빈값을_반환한다() {
        when(healthTopicResolver.resolve("아무 관련 없는 이야기")).thenReturn(Optional.empty());

        assertThat(service.search("아무 관련 없는 이야기")).isEmpty();
    }

    @Test
    void 상황명은_찾았지만_검색결과가_없으면_빈값을_반환한다() {
        when(healthTopicResolver.resolve("화상 관련 텍스트")).thenReturn(Optional.of("화상"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of());

        assertThat(service.search("화상 관련 텍스트")).isEmpty();
    }

    @Test
    void 참고자료에_현장_조치가_없으면_빈값을_반환한다() {
        when(healthTopicResolver.resolve("감기 기운이 있어요")).thenReturn(Optional.of("감기"));
        Document doc = new Document("감기 관련 내용", Map.of("disease", "감기"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (감기) ...");
        stubChatClientEntity(new RawFirstAidGuide("감기 응급처치", List.of()));

        assertThat(service.search("감기 기운이 있어요")).isEmpty();
    }

    // 같은 topic·같은 참고자료에도 LLM이 한 번은 steps를 빈 배열로, 한 번은 채워서 응답하는 경우가
    // 실측으로 확인됐다(샘플링 편차) - 첫 시도가 빈 배열이어도 재시도해서 살려내야 한다.
    @Test
    void steps가_빈_배열이면_재시도해서_다음_시도의_결과를_쓴다() {
        when(healthTopicResolver.resolve("아이가 이물질을 삼켰어요")).thenReturn(Optional.of("가정 내 아동안전"));
        Document doc = new Document("가정 내 아동안전 관련 내용", Map.of("disease", "가정 내 아동안전"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (가정 내 아동안전) ...");
        stubChatClientEntitySequence(
                new RawFirstAidGuide("가정 내 아동안전 응급처치", List.of()),
                new RawFirstAidGuide("가정 내 아동안전 응급처치", List.of("즉시 병원을 방문합니다.")));

        var result = service.search("아이가 이물질을 삼켰어요");

        assertThat(result).isPresent();
        assertThat(result.get().steps()).containsExactly("즉시 병원을 방문합니다.");
    }

    // 화상 케이스에서 실측으로 확인된 문제: 참고자료에 "전기 스위치 차단"처럼 지금도 계속 위험을 주는
    // 요인을 없애는 조치가 있어도, 참고자료 안에서 맨 뒤에 있으면 프롬프트 지시만으로는 모델이 그
    // 원문 순서를 그대로 따라가 버려서 항상 맨 뒤에 남았다 - 그래서 buildContext에 넘기기 전에
    // 코드가 순서를 강제로 바꾼다.
    @Test
    void 위험제거_문서를_참고자료_맨_앞으로_옮겨서_전달한다() {
        when(healthTopicResolver.resolve("화상 입었어요")).thenReturn(Optional.of("화상"));
        Document coolWater = new Document("환부를 흐르는 찬물로 20분 이상 식힙니다.", Map.of("disease", "화상"));
        Document blister = new Document("물집은 터뜨리지 않습니다.", Map.of("disease", "화상"));
        Document cutPower = new Document("전기로 인한 화상의 경우 전기 스위치를 내려 전기 공급을 차단합니다.", Map.of("disease", "화상"));
        // 검색 결과 자체가 위험제거 문서(cutPower)를 맨 뒤에 두고 있는 상황을 재현한다.
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(coolWater, blister, cutPower));
        when(ragRetrievalService.buildContext(anyList())).thenReturn("...");
        stubChatClientEntity(new RawFirstAidGuide("화상 응급처치", List.of("전기 스위치를 내려 차단합니다.")));

        service.search("화상 입었어요");

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).buildContext(captor.capture());
        List<Document> passed = captor.getValue();
        assertThat(passed).hasSize(3);
        assertThat(passed.get(0)).isEqualTo(cutPower); // 위험제거 문서가 맨 앞으로
        // 나머지 문서들의 상대 순서는 그대로 유지된다(안정 정렬).
        assertThat(passed.get(1)).isEqualTo(coolWater);
        assertThat(passed.get(2)).isEqualTo(blister);
    }

    // 실측으로 확인된 진짜 원인: 화상의 "치료" 섹션은 화학/흡입/전기 화상 설명이 개행으로만 문단이
    // 나뉜 채 하나의 문서로 통째로 인덱싱되어 있다 - 그래서 문서 단위 재배열(위 테스트)만으로는 부족하고,
    // 문서 텍스트 안의 문단도 재배열해야 한다.
    @Test
    void 한_문서_안에_있는_위험제거_문단도_맨_앞으로_옮긴다() {
        when(healthTopicResolver.resolve("화상 입었어요")).thenReturn(Optional.of("화상"));
        String treatmentText = "환자를 안전한 곳으로 옮기는 것이 우선입니다. 환부를 찬물로 식힙니다.\n"
                + "화학 약품에 의한 화상의 경우 의류를 제거합니다.\n"
                + "전기로 인한 화상의 경우 전기 스위치를 내려 전기 공급을 차단해야 합니다.";
        Document treatment = new Document(treatmentText, Map.of("disease", "화상"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(treatment));
        when(ragRetrievalService.buildContext(anyList())).thenReturn("...");
        stubChatClientEntity(new RawFirstAidGuide("화상 응급처치", List.of("전기 스위치를 내려 차단합니다.")));

        service.search("화상 입었어요");

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(ragRetrievalService).buildContext(captor.capture());
        String passedText = captor.getValue().get(0).getText();
        assertThat(passedText).startsWith("전기로 인한 화상의 경우 전기 스위치를 내려 전기 공급을 차단해야 합니다.");
    }

    @Test
    void getBySituation은_원래_상황명을_그대로_응답에_돌려준다() {
        // 프론트 칩 라벨("기도막힘")과 코퍼스 실제 문서명("심폐소생술(이물질에 의한 기도폐쇄의 처치)")이
        // 다를 수 있다 - 검색은 코퍼스 문서명으로 하되, 응답의 situation은 호출자가 준 라벨 그대로다.
        when(healthTopicResolver.resolve("기도막힘")).thenReturn(Optional.of("심폐소생술(이물질에 의한 기도폐쇄의 처치)"));
        Document doc = new Document("기도막힘 관련 내용", Map.of("disease", "심폐소생술(이물질에 의한 기도폐쇄의 처치)"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] ...");
        stubChatClientEntity(new RawFirstAidGuide("기도막힘 응급처치", List.of("등을 두드립니다.")));

        var result = service.getBySituation("기도막힘");

        assertThat(result.situation()).isEqualTo("기도막힘");
        assertThat(result.steps()).containsExactly("등을 두드립니다.");
    }

    @Test
    void 상황명을_못_찾으면_예외() {
        when(healthTopicResolver.resolve("알수없음")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySituation("알수없음")).isInstanceOf(IllegalArgumentException.class);
    }

    // 매 요청 LLM을 다시 불러서 느리다는 지적(실측 확인)에 따라 topic별로 캐싱했다 - 같은 topic으로
    // 두 번 요청해도 가이드 생성 LLM 호출(chatClient.prompt())은 한 번만 나가야 한다.
    @Test
    void 같은_topic은_두번째_요청부터_LLM을_다시_부르지_않는다() {
        when(healthTopicResolver.resolve("손에 화상을 입었어요")).thenReturn(Optional.of("화상"));
        Document doc = new Document("화상 관련 내용", Map.of("disease", "화상"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of(doc));
        when(ragRetrievalService.buildContext(List.of(doc))).thenReturn("[1] (화상) ...");
        stubChatClientEntity(new RawFirstAidGuide("화상 응급처치", List.of("찬물로 식힙니다.")));

        var first = service.search("손에 화상을 입었어요");
        var second = service.search("손에 화상을 입었어요");

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get().steps()).isEqualTo(first.get().steps());
        verify(chatClient, times(1)).prompt();
    }

    // 실패(참고자료 없음)는 캐싱하지 않는다 - 두 번째 시도 때도 다시 검색을 시도해야 한다(예: 그
    // 사이에 재인덱싱으로 콘텐츠가 생겼을 수 있으므로).
    @Test
    void 검색결과가_없는_topic은_캐싱하지_않고_매번_다시_검색한다() {
        when(healthTopicResolver.resolve("화상 관련 텍스트")).thenReturn(Optional.of("화상"));
        when(ragRetrievalService.retrieveByExactDisease(anyString())).thenReturn(List.of());

        service.search("화상 관련 텍스트");
        service.search("화상 관련 텍스트");

        verify(ragRetrievalService, times(2)).retrieveByExactDisease("화상");
    }

    @SuppressWarnings("unchecked")
    private void stubChatClientEntity(RawFirstAidGuide value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawFirstAidGuide.class)).thenReturn(value);
    }

    // 재시도 시도 순서대로 다른 값을 돌려주기 위한 버전 - Mockito의 thenReturn(first, rest...)는
    // 마지막 인자를 그 뒤 모든 호출에 반복해서 돌려준다.
    @SuppressWarnings("unchecked")
    private void stubChatClientEntitySequence(RawFirstAidGuide first, RawFirstAidGuide... rest) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawFirstAidGuide.class)).thenReturn(first, rest);
    }
}
