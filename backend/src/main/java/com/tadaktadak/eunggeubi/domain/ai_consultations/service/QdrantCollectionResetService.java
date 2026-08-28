package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// 전체 재인덱싱 시작 전에 Qdrant 컬렉션을 삭제 후 같은 스펙으로 재생성해서 중복 적재를 막는다.
// Spring AI VectorStore 인터페이스엔 "컬렉션 통째로 비우기"가 없어서 Qdrant REST API를 직접 호출한다.
// (참고: 이 REST 포트(6333)는 spring.ai.vectorstore.qdrant.port(6334, gRPC용)와 다른 포트다.)
@Component
public class QdrantCollectionResetService {

    private final RestClient restClient;
    private final String collectionName;
    private final int dimensions;

    public QdrantCollectionResetService(
            RestClient.Builder restClientBuilder,
            @Value("${qdrant.rest-base-url:http://localhost:6333}") String restBaseUrl,
            @Value("${spring.ai.vectorstore.qdrant.collection-name}") String collectionName,
            @Value("${spring.ai.openai.embedding.options.dimensions}") int dimensions) {
        this.restClient = restClientBuilder.baseUrl(restBaseUrl).build();
        this.collectionName = collectionName;
        this.dimensions = dimensions;
    }

    public void resetCollection() {
        // 컬렉션이 없어도 Qdrant가 에러 없이 응답하므로 존재 여부를 미리 확인할 필요는 없다.
        restClient.delete()
                .uri("/collections/{name}", collectionName)
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> body = Map.of(
                "vectors", Map.of("size", dimensions, "distance", "Cosine")
        );
        restClient.put()
                .uri("/collections/{name}", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
