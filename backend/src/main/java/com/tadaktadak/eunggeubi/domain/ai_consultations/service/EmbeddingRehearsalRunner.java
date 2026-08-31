package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// KDCA_TOKEN 없이도 "OpenAI 임베딩 -> Qdrant 저장 -> 유사도 검색" 절반이 실제로 동작하는지
// 미리 확인해보는 1회성 리허설. KDCA에서 받아온 진짜 데이터가 아니라, 프론트 mock(aiConsultations.ts)에
// 있던 샘플 문단을 그대로 재사용한다. 실제 OpenAI API를 호출하므로 소액 비용이 발생한다.
// --spring.profiles.active=rehearsal 로 켤 때만 실행되고, 평소엔 안 돈다.
@Slf4j
@Component
@Profile("rehearsal")
public class EmbeddingRehearsalRunner implements CommandLineRunner {

    private final VectorStore vectorStore;

    public EmbeddingRehearsalRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        log.info("=== OpenAI 임베딩 + Qdrant 저장/검색 리허설 시작 (샘플 텍스트, 실제 KDCA 데이터 아님) ===");

        List<Document> samples = List.of(
                new Document(
                        "두통은 긴장성, 편두통, 군발성 등 다양한 원인으로 알려져 있어요. 대부분 휴식으로 호전된다고 알려져 있지만, "
                                + "갑작스럽고 심한 두통은 주의가 필요해요.",
                        Map.of("source", "리허설-샘플", "disease", "두통", "section", "개요", "cntntsSn", "sample-1")),
                new Document(
                        "복통은 소화불량, 장염, 생리통 등 흔한 원인부터 주의가 필요한 원인까지 다양해요. "
                                + "통증 위치와 양상에 따라 원인이 크게 달라질 수 있어요.",
                        Map.of("source", "리허설-샘플", "disease", "복통", "section", "개요", "cntntsSn", "sample-2")),
                new Document(
                        "발열은 감염에 대한 우리 몸의 정상적인 반응인 경우가 많아요. 수분을 충분히 섭취하고 휴식하면 대부분 "
                                + "호전되지만, 고열이 지속되면 진료가 필요해요.",
                        Map.of("source", "리허설-샘플", "disease", "발열", "section", "개요", "cntntsSn", "sample-3")),
                new Document(
                        "어지럼은 이석증 같은 귀 문제, 저혈압, 빈혈 등 다양한 원인으로 나타날 수 있어요. "
                                + "대부분 크게 위험하지 않지만 반복되면 원인 확인이 필요해요.",
                        Map.of("source", "리허설-샘플", "disease", "어지럼", "section", "개요", "cntntsSn", "sample-4"))
        );

        vectorStore.add(samples);
        log.info("샘플 {}건 임베딩 + Qdrant 저장 완료", samples.size());

        String query = "머리가 아파요";
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).build());

        log.info("검색어: \"{}\" -> 상위 {}건", query, results.size());
        for (int i = 0; i < results.size(); i++) {
            Document d = results.get(i);
            String preview = d.getText().length() > 30 ? d.getText().substring(0, 30) + "..." : d.getText();
            log.info("  {}위 disease={} score={} content=\"{}\"",
                    i + 1, d.getMetadata().get("disease"), d.getScore(), preview);
        }

        log.info("=== 리허설 종료 (실제 663개 인덱싱 전에 --spring.profiles.active=index 로 컬렉션이 다시 초기화됩니다) ===");
    }
}
