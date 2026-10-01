package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawSearchQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RagRetrievalServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final RagRetrievalService service = new RagRetrievalService(vectorStore, chatClient);

    @Test
    void 검색어_재작성이_성공하면_재작성된_검색어를_반환한다() {
        stubChatClientEntity(new RawSearchQuery("부종 다리 발목 붓기"));

        assertThat(service.rewriteForSearch("발이 부어요")).isEqualTo("부종 다리 발목 붓기");
    }

    @Test
    void 검색어_재작성_호출이_실패하면_원래_질문을_반환한다() {
        // entity(RawSearchQuery.class)를 일부러 스텁하지 않음 - 모키토 기본값(null)으로 재작성 실패를 흉내낸다.
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        assertThat(service.rewriteForSearch("발이 부어요")).isEqualTo("발이 부어요");
    }

    @Test
    void 짧은_단어가_threshold_밑이어도_질병명이_정확히_일치하면_결과에_포함된다() {
        Document exactDisease = doc("1", "골절", "정의", "뼈가 부러진 상태를 말한다.", 0.28);
        Document irrelevant = doc("2", "당뇨병", "치료", "혈당을 조절하는 치료가 필요하다.", 0.40);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(irrelevant, exactDisease));

        List<Document> result = service.retrieve("골절");

        assertThat(result).extracting(Document::getId).contains("1");
    }

    @Test
    void 키워드_매치도_없고_threshold도_미달인_노이즈_문서는_제외된다() {
        Document noise = doc("1", "당뇨병", "치료", "혈당을 조절하는 치료가 필요하다.", 0.20);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(noise));

        List<Document> result = service.retrieve("골절");

        assertThat(result).isEmpty();
    }

    @Test
    void 벡터_순위는_낮지만_질병명이_일치하는_문서가_RRF로_더_앞에_온다() {
        Document vectorTop = doc("top", "당뇨병", "치료", "혈당을 조절하는 치료가 필요하다.", 0.50);
        Document exactDisease = doc("match", "골절", "치료", "부목으로 고정한다.", 0.36);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(vectorTop, exactDisease));

        List<Document> result = service.retrieve("골절");

        assertThat(result).extracting(Document::getId).containsExactly("match", "top");
    }

    // 483개 질병명 전수 벤치마크(실측)에서 발견한 케이스 - 정답 문서(질병명 완전일치)의 벡터 순위가
    // 아주 깊으면(예: "만성비염"이 38위), RRF만으로는 "키워드 매치는 없지만 벡터 순위가 훨씬 좋은"
    // 문서들에 밀려서 top-K 밖으로 나가버린다. 이 테스트는 그 상황을 후보 11개(벡터 순위 1~10위는
    // 무관한 문서, 11위가 질병명 완전일치)로 재현해서, 완전일치 우선 규칙이 없으면 실패했을 케이스다.
    @Test
    void 벡터_순위가_아주_깊어도_질병명_완전일치는_최우선으로_온다() {
        List<Document> candidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            candidates.add(doc("irrelevant-" + i, "당뇨병", "치료", "혈당을 조절하는 치료가 필요하다.", 0.50 - i * 0.01));
        }
        candidates.add(doc("exact", "만성비염", "원인", "코 점막에 만성적인 염증이 생긴 상태이다.", 0.20));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(candidates);

        List<Document> result = service.retrieve("만성비염");

        assertThat(result.get(0).getId()).isEqualTo("exact");
    }

    @Test
    void retrieveGrouped은_해석별로_따로_검색하고_중복을_제거한다() {
        Document shared = doc("shared", "거북목증후군", "정의", "거북목증후군에 대한 설명이다.", 0.60);
        Document neckOnly = doc("neck", "거북목증후군", "치료", "자세 교정이 필요하다.", 0.55);
        Document throatOnly = doc("throat", "인두염", "치료", "인두염 치료 방법이다.", 0.50);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0);
            if (request.getQuery().contains("거북목")) {
                return List.of(shared, neckOnly);
            }
            return List.of(shared, throatOnly);
        });

        List<List<Document>> groups = service.retrieveGrouped("목 통증 거북목증후군|인후통 인두염");

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0)).extracting(Document::getId).containsExactly("shared", "neck");
        assertThat(groups.get(1)).extracting(Document::getId).containsExactly("throat");
    }

    @Test
    void 검색_결과가_없으면_빈_리스트를_반환한다() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertThat(service.retrieve("골절")).isEmpty();
    }

    private static Document doc(String id, String disease, String section, String text, double score) {
        return Document.builder()
                .id(id)
                .text(text)
                .metadata(Map.of("disease", disease, "section", section))
                .score(score)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubChatClientEntity(RawSearchQuery value) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(RawSearchQuery.class)).thenReturn(value);
    }
}
