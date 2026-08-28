package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 실제로 인덱싱된 health_info 컬렉션에 검색만 해보는 1회성 점검용.
// (EmbeddingRehearsalRunner와 달리 데이터를 추가하지 않는다 - 실 데이터에 장난감 데이터가 섞이면 안 되므로)
// 검색어는 --kdca.health-info.search-query="..." 로 넘긴다. 기본값은 "머리가 아파요".
// --spring.profiles.active=search-check 로 켤 때만 실행된다.
@Slf4j
@Component
@Profile("search-check")
public class HealthInfoSearchCheckRunner implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final String query;

    public HealthInfoSearchCheckRunner(VectorStore vectorStore,
                                        @Value("${kdca.health-info.search-query:머리가 아파요}") String query) {
        this.vectorStore = vectorStore;
        this.query = query;
    }

    @Override
    public void run(String... args) {
        log.info("=== 검색 점검: \"{}\" ===", query);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(5).build());

        if (results.isEmpty()) {
            log.warn("검색 결과가 없습니다.");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            Document d = results.get(i);
            String preview = d.getText().length() > 60 ? d.getText().substring(0, 60) + "..." : d.getText();
            log.info("  {}위 disease={} section={} score={}\n      \"{}\"",
                    i + 1, d.getMetadata().get("disease"), d.getMetadata().get("section"), d.getScore(), preview);
        }
    }
}
