package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

// health_info 컬렉션 검색 로직. AiConsultationService(1차 답변)와 ConsultationRegenerationService(재생성)가
// 똑같은 검색+컨텍스트 조립 방식을 써야 해서 공용으로 뽑아냈다.
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    // 질문과 관련 없는 문서가 답변에 섞이는 걸 막기 위한 최소 유사도. 이 밑으로는 검색 결과에서 제외한다.
    private static final double SIMILARITY_THRESHOLD = 0.35;
    // 5에서 8로 늘렸다 - 검색어 재작성(AiConsultationService.rewriteQueryForSearch)을 거쳐도 실제로
    // 도움 되는 문서가 top 5 밖에 걸리는 경우가 있었다. 후보를 늘려도 안전한 이유: CONSULT_PROMPT가
    // "참고자료에 없는 내용은 답하지 않는다"를 강제해서, 무관한 후보가 몇 개 섞여도 모델이 그냥
    // 무시할 뿐 답변에 실제로 쓰이진 않는다(sourceIndexes로 인용된 것만 "출처"로 노출됨).
    private static final int TOP_K = 8;

    private final VectorStore vectorStore;

    public List<Document> retrieve(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build());
    }

    // LLM 프롬프트에 넣을 "[번호] (질병명) 본문" 형태의 컨텍스트 문자열. 번호는 1부터 시작하고,
    // 이 번호가 그대로 인용 근거 번호(sourceIndexes)로 쓰인다.
    public String buildContext(List<Document> docs) {
        return IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> "[%d] (%s) %s".formatted(
                        i, docs.get(i - 1).getMetadata().get("disease"), docs.get(i - 1).getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
