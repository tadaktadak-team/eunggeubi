package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

// 토큰/실제 네트워크 없이, KdcaHealthInfoClient를 mock으로 대체해서
// "seed.csv 순회 -> 성공은 저장, 실패는 스킵 -> 컬렉션 초기화가 먼저 호출됨" 오케스트레이션만 검증한다.
// 실제 KDCA 호출은 별도로 검증 완료(루트 태그, 인코딩, 타임아웃 이슈 전부 반영됨).
class HealthInfoIndexingRunnerTest {

    // fixture XML을 KdcaHealthInfoResponse로 변환할 때만 쓰는 실제 파서 (mock 아님)
    private final KdcaHealthInfoClient fixtureParser =
            new KdcaHealthInfoClient(RestClient.builder(), "http://dummy", "dummy-token");
    private final HealthInfoDocumentMapper documentMapper = new HealthInfoDocumentMapper();

    @Test
    void seed_csv를_순회하며_성공은_저장하고_실패는_스킵한다() throws Exception {
        KdcaHealthInfoResponse multi = fixtureParser.parse(readFixture("kdca-detail-multi.xml"));   // 5684, 섹션 2개(필터 후)
        KdcaHealthInfoResponse single = fixtureParser.parse(readFixture("kdca-detail-single.xml"));  // 9999, 섹션 1개
        KdcaHealthInfoResponse error = fixtureParser.parse(readFixture("kdca-detail-error.xml"));    // 1111, 실패 응답

        KdcaHealthInfoClient mockClient = mock(KdcaHealthInfoClient.class);
        when(mockClient.fetchDetail("5684")).thenReturn(multi);
        when(mockClient.fetchDetail("9999")).thenReturn(single);
        when(mockClient.fetchDetail("1111")).thenReturn(error);

        VectorStore mockVectorStore = mock(VectorStore.class);
        QdrantCollectionResetService mockResetService = mock(QdrantCollectionResetService.class);
        Resource seedResource = new ClassPathResource("fixtures/seed-sample.csv");

        HealthInfoIndexingRunner runner = new HealthInfoIndexingRunner(
                mockClient, documentMapper, mockVectorStore, mockResetService, seedResource, 0L, 0);

        runner.run();

        // 인덱싱 시작 전에 컬렉션을 반드시 초기화해야 한다
        verify(mockResetService, times(1)).resetCollection();

        // 성공한 2건(5684, 9999)만 vectorStore.add가 호출되고, 실패한 1건(1111)은 스킵된다
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockVectorStore, times(2)).add(captor.capture());

        List<List<Document>> addedBatches = captor.getAllValues();
        assertThat(addedBatches.get(0)).hasSize(2); // 직장탈출증: 개요/증상만 (사진/빈값 제외)
        assertThat(addedBatches.get(1)).hasSize(1); // 딸꾹질: 개요 1개
    }

    @Test
    void CSV_인용규칙을_따르는_콤마_포함_질병명도_정확히_읽는다() throws Exception {
        // seed.csv 실제 데이터 중 질병명 자체에 콤마가 들어있는 행은 정식 CSV 규칙대로
        // 큰따옴표로 감싸져 있다("...,..."). Commons CSV가 이 인용 규칙을 제대로 처리하는지 확인한다.
        HealthInfoIndexingRunner runner = new HealthInfoIndexingRunner(
                mock(KdcaHealthInfoClient.class), documentMapper, mock(VectorStore.class),
                mock(QdrantCollectionResetService.class),
                new ClassPathResource("fixtures/seed-quoted-sample.csv"), 0L, 0);

        List<HealthInfoIndexingRunner.SeedEntry> entries = runner.readSeed();

        assertThat(entries).containsExactly(
                new HealthInfoIndexingRunner.SeedEntry("5969", "“무릎관절염, 올바르게 운동하기”"),
                new HealthInfoIndexingRunner.SeedEntry("5494", "“흉곽기형(오목가슴, 함몰흉, 누두흉)”"),
                new HealthInfoIndexingRunner.SeedEntry("2847", "“사무용품에 의한 오존, 휘발성유기화합물(VOCs),  중금속”"),
                new HealthInfoIndexingRunner.SeedEntry("5423", "감기")
        );
    }

    private String readFixture(String name) throws IOException {
        Path path = new ClassPathResource("fixtures/" + name).getFile().toPath();
        return Files.readString(path);
    }
}
