package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

// 네트워크 없이, 저장해둔 fixture XML 문자열만으로 파싱 로직을 검증한다.
// 실제 KDCA 응답으로 검증 완료(루트 태그 <XML>, UTF-8 인코딩 등 반영됨).
class KdcaHealthInfoClientTest {

    private final KdcaHealthInfoClient client =
            new KdcaHealthInfoClient(RestClient.builder(), "http://dummy", "dummy-token");

    @Test
    void 여러_섹션이_있는_응답을_파싱한다() throws IOException {
        String xml = readFixture("kdca-detail-multi.xml");

        KdcaHealthInfoResponse response = client.parse(xml);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSvc().getDiseaseName()).isEqualTo("직장탈출증");
        assertThat(response.getSvc().getCntntsSn()).isEqualTo("5684");
        assertThat(response.getSvc().getSections()).hasSize(4); // 필터링 전 원본 개수
        assertThat(response.getSvc().getSections().get(0).getName()).isEqualTo("개요");
    }

    @Test
    void 섹션이_1개뿐이어도_리스트로_파싱된다() throws IOException {
        String xml = readFixture("kdca-detail-single.xml");

        KdcaHealthInfoResponse response = client.parse(xml);

        assertThat(response.getSvc().getSections()).hasSize(1);
        assertThat(response.getSvc().getSections().get(0).getContent())
                .contains("횡격막이 불수의적으로 수축");
    }

    @Test
    void 에러_응답이면_isSuccess가_false다() throws IOException {
        String xml = readFixture("kdca-detail-error.xml");

        KdcaHealthInfoResponse response = client.parse(xml);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getHead().getMessage()).isEqualTo("잘못된 요청입니다.");
    }

    private String readFixture(String name) throws IOException {
        Path path = new ClassPathResource("fixtures/" + name).getFile().toPath();
        return Files.readString(path);
    }
}
