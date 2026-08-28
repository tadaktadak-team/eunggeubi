package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// seed.csv(cntntsSn 663개)를 순회하며 KDCA에서 건강정보를 받아 Qdrant에 적재하는 1회성 배치.
// 매 부팅마다 돌면 안 되므로 --spring.profiles.active=index 로 명시적으로 켤 때만 실행된다.
// 요청 사이 딜레이는 KDCA 서버 요청 제한을 피하기 위함.
@Slf4j
@Component
@Profile("index")
public class HealthInfoIndexingRunner implements CommandLineRunner {

    private final KdcaHealthInfoClient kdcaClient;
    private final HealthInfoDocumentMapper documentMapper;
    private final VectorStore vectorStore;
    private final QdrantCollectionResetService collectionResetService;
    private final Resource seedResource;
    private final long requestIntervalMs;
    private final int maxDocs;

    public HealthInfoIndexingRunner(KdcaHealthInfoClient kdcaClient,
                                     HealthInfoDocumentMapper documentMapper,
                                     VectorStore vectorStore,
                                     QdrantCollectionResetService collectionResetService,
                                     @Value("${kdca.health-info.seed-file:classpath:seed.csv}") Resource seedResource,
                                     @Value("${kdca.health-info.request-interval-ms:300}") long requestIntervalMs,
                                     @Value("${kdca.health-info.max-docs:0}") int maxDocs) {
        this.kdcaClient = kdcaClient;
        this.documentMapper = documentMapper;
        this.vectorStore = vectorStore;
        this.collectionResetService = collectionResetService;
        this.seedResource = seedResource;
        this.requestIntervalMs = requestIntervalMs;
        this.maxDocs = maxDocs;
    }

    @Override
    public void run(String... args) throws Exception {
        List<SeedEntry> seedEntries = readSeed();
        // maxDocs > 0이면 소량 검증용으로 앞에서부터 그만큼만 자른다 (기본 0 = 전체 663개).
        if (maxDocs > 0 && seedEntries.size() > maxDocs) {
            seedEntries = seedEntries.subList(0, maxDocs);
            log.info("kdca.health-info.max-docs={} 설정으로 앞 {}건만 소량 실행합니다.", maxDocs, maxDocs);
        }
        log.info("KDCA 건강정보 인덱싱 시작: 총 {}건", seedEntries.size());

        collectionResetService.resetCollection();
        log.info("Qdrant 컬렉션을 비우고 새로 만들었습니다.");

        int successCount = 0;
        int skippedCount = 0;
        int totalChunks = 0;

        for (int i = 0; i < seedEntries.size(); i++) {
            SeedEntry entry = seedEntries.get(i);
            int progress = i + 1;
            try {
                KdcaHealthInfoResponse response = kdcaClient.fetchDetail(entry.cntntsSn());

                if (!response.isSuccess()) {
                    String message = response.getHead() != null ? response.getHead().getMessage() : "unknown";
                    log.warn("[{}/{}] 실패 응답 cntntsSn={} disease={} message={}",
                            progress, seedEntries.size(), entry.cntntsSn(), entry.disease(), message);
                    skippedCount++;
                    continue;
                }

                List<Document> documents = documentMapper.toDocuments(response);
                if (documents.isEmpty()) {
                    log.warn("[{}/{}] 저장할 섹션 없음 cntntsSn={} disease={}",
                            progress, seedEntries.size(), entry.cntntsSn(), entry.disease());
                    skippedCount++;
                    continue;
                }

                vectorStore.add(documents);
                successCount++;
                totalChunks += documents.size();
                log.info("[{}/{}] 적재 완료 disease={} chunks={}",
                        progress, seedEntries.size(), entry.disease(), documents.size());
            } catch (Exception e) {
                log.error("[{}/{}] 처리 실패 cntntsSn={} disease={}: {}",
                        progress, seedEntries.size(), entry.cntntsSn(), entry.disease(), e.getMessage());
                skippedCount++;
            }

            if (requestIntervalMs > 0) {
                Thread.sleep(requestIntervalMs);
            }
        }

        log.info("KDCA 건강정보 인덱싱 완료: 성공 {}건 / 스킵 {}건 / 총 청크 {}개",
                successCount, skippedCount, totalChunks);
    }

    // seed.csv는 질병명에 콤마가 포함된 행(예: "굴절이상(근시, 원시, 난시)")이 있고,
    // 이런 행은 정식 CSV 규칙대로 큰따옴표로 감싸져 있다("...,..."). 직접 콤마로 쪼개면 이 인용 규칙을
    // 놓치기 쉬워서, RFC4180을 제대로 구현한 Commons CSV로 파싱한다. 헤더는 "cntntsSn,disease".
    List<SeedEntry> readSeed() throws IOException {
        try (Reader reader = new InputStreamReader(seedResource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            List<SeedEntry> entries = new ArrayList<>();
            for (CSVRecord record : parser) {
                String cntntsSn = record.get("cntntsSn");
                String disease = record.get("disease");
                if (cntntsSn == null || cntntsSn.isBlank()) {
                    continue;
                }
                entries.add(new SeedEntry(cntntsSn.trim(), disease != null ? disease.trim() : ""));
            }
            return entries;
        }
    }

    record SeedEntry(String cntntsSn, String disease) {
    }
}
