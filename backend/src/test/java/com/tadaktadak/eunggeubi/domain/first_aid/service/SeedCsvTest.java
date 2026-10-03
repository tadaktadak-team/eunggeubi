package com.tadaktadak.eunggeubi.domain.first_aid.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

// seed.csv의 disease 값은 HealthTopicResolver가 그대로 돌려주고, RagRetrievalService.
// retrieveByExactDisease가 Qdrant disease 메타데이터와 완전 일치로 비교한다 - 실측으로 494개 중
// 20개가 실제 Qdrant 값과 안 맞았던 적이 있다(유니코드 곡선따옴표가 잘못 붙거나, 콘텐츠가 아예
// 인덱싱 안 된 경우). 그 원인 패턴이 재발하는지만 정적으로 감지한다 - Qdrant와의 실시간 동기화
// 여부까지는 이 테스트로 확인 못 한다(통합 테스트 인프라 없음).
class SeedCsvTest {

    @Test
    void 행_수가_예상과_같다() throws IOException {
        assertThat(readDiseaseNames()).hasSize(483);
    }

    @Test
    void 유니코드_곡선따옴표가_섞인_이름이_없다() throws IOException {
        assertThat(readDiseaseNames()).noneMatch(name -> name.contains("“") || name.contains("”"));
    }

    @Test
    void 연속된_공백이_들어간_이름이_없다() throws IOException {
        assertThat(readDiseaseNames()).noneMatch(name -> name.contains("  "));
    }

    private List<String> readDiseaseNames() throws IOException {
        try (Reader reader = new InputStreamReader(
                new ClassPathResource("seed.csv").getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            List<String> names = new ArrayList<>();
            for (CSVRecord record : parser) {
                String disease = record.get("disease");
                if (disease != null && !disease.isBlank()) {
                    names.add(disease);
                }
            }
            return names;
        }
    }
}
