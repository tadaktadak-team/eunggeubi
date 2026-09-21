package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// 실제로 인덱싱된 health_info 컬렉션에 검색만 해보는 1회성 점검용.
// (EmbeddingRehearsalRunner와 달리 데이터를 추가하지 않는다 - 실 데이터에 장난감 데이터가 섞이면 안 되므로)
// 검색어는 --kdca.health-info.search-query="검색어1,검색어2,..." 로 콤마 구분해 여러 개 넘길 수 있다.
// 기본값은 사용자가 흔히 입력할 법한 증상 문장들이다.
// --spring.profiles.active=search-check 로 켤 때만 실행된다.
// RagRetrievalService.retrieve()를 그대로 거친다 - 검색어 재작성(LLM 호출)까지 섞으면 순수 검색
// 로직 변경의 효과를 비교하기 어려워지므로, 재작성 없이 원문 그대로 검색해서 검색 로직만 점검한다.
@Slf4j
@Component
@Profile("search-check")
public class HealthInfoSearchCheckRunner implements CommandLineRunner {

    private final RagRetrievalService ragRetrievalService;
    private final List<String> queries;

    public HealthInfoSearchCheckRunner(
            RagRetrievalService ragRetrievalService,
            @Value("#{'${kdca.health-info.search-query:머리가 아파요,배가 아파요,열이 나요,기침이 나요,목이 아파요,"
                    + "어지러워요,숨이 차요,가슴이 답답해요,설사를 해요,피부가 가려워요,허리가 아파요,무릎이 아파요}'"
                    + ".split(',')}") List<String> queries) {
        this.ragRetrievalService = ragRetrievalService;
        this.queries = queries;
    }

    @Override
    public void run(String... args) {
        for (String query : queries) {
            log.info("=== 검색 점검: \"{}\" ===", query);

            List<Document> results = ragRetrievalService.retrieve(query);

            if (results.isEmpty()) {
                log.warn("검색 결과가 없습니다.");
                continue;
            }

            for (int i = 0; i < results.size(); i++) {
                Document d = results.get(i);
                String preview = d.getText().length() > 60 ? d.getText().substring(0, 60) + "..." : d.getText();
                log.info("  {}위 disease={} section={} score={}\n      \"{}\"",
                        i + 1, d.getMetadata().get("disease"), d.getMetadata().get("section"), d.getScore(), preview);
            }
        }
    }
}
