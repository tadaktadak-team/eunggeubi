package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class HealthInfoDocumentMapperTest {

    private final KdcaHealthInfoClient client =
            new KdcaHealthInfoClient(RestClient.builder(), "http://dummy", "dummy-token");
    private final HealthInfoDocumentMapper mapper = new HealthInfoDocumentMapper();

    @Test
    void 빈_본문과_이미지URL_섹션은_제외하고_변환한다() throws IOException {
        KdcaHealthInfoResponse response = client.parse(readFixture("kdca-detail-multi.xml"));
        // fixture에는 섹션 4개(개요/증상/사진(http)/참고사항(빈 값))가 있고,
        // "사진"과 "참고사항"은 제외되어 2개만 Document로 변환되어야 한다.

        List<Document> documents = mapper.toDocuments(response);

        assertThat(documents).hasSize(2);
        assertThat(documents).extracting(d -> d.getMetadata().get("section"))
                .containsExactly("개요", "증상");
        assertThat(documents).allSatisfy(d -> {
            assertThat(d.getMetadata().get("source")).isEqualTo("질병관리청 국가건강정보포털");
            assertThat(d.getMetadata().get("disease")).isEqualTo("직장탈출증");
            assertThat(d.getMetadata().get("cntntsSn")).isEqualTo("5684");
        });
    }

    @Test
    void 섹션이_1개면_Document_1개로_변환된다() throws IOException {
        KdcaHealthInfoResponse response = client.parse(readFixture("kdca-detail-single.xml"));

        List<Document> documents = mapper.toDocuments(response);

        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getText()).contains("횡격막");
    }

    @Test
    void 섹션이_너무_길면_여러_Document로_쪼개진다() throws IOException {
        // 실제 KDCA 응답 중 "자주하는 질문"(9023자), "참고문헌"(14592자) 섹션이 너무 길어서
        // 그대로 임베딩에 넣으면 OpenAI 토큰 한도를 넘겨 통째로 실패하던 문제를 재현한 fixture.
        KdcaHealthInfoResponse response = client.parse(readFixture("kdca-detail-long-section.xml"));

        List<Document> documents = mapper.toDocuments(response);

        assertThat(documents.size()).isGreaterThan(1);
        assertThat(documents).allSatisfy(d -> assertThat(d.getText().length()).isLessThanOrEqualTo(3000));
        assertThat(documents).extracting(d -> d.getMetadata().get("section"))
                .allSatisfy(section -> assertThat((String) section).startsWith("자주하는 질문 ("));
    }

    private String readFixture(String name) throws IOException {
        Path path = new ClassPathResource("fixtures/" + name).getFile().toPath();
        return Files.readString(path);
    }
}
