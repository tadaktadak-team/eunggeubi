package com.tadaktadak.eunggeubi.domain.first_aid.service;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicMatch;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// health_info 컬렉션에 실제로 인덱싱된 질병/상황명 전체(seed.csv, ~494개) 목록을 LLM에게 주고,
// 자유 텍스트와 의미가 가장 가까운 이름을 고르게 한다.
//
// 처음엔 이름끼리 임베딩 유사도로 찾는 방식(HealthTopicIndex)을 썼는데, "화상"처럼 검색어가 코퍼스
// 문서명과 거의 같을 땐 잘 맞아도(유사도 0.999) "기도막힘"처럼 표현이 다른 경우(실제 문서명은
// "기도폐쇄" 계열)엔 유사도가 0.4~0.5대로 떨어지면서 엉뚱한 질병에 매칭되는 게 실측으로 확인됐다.
// 일반 목적 임베딩이 이런 의료 동의어 관계까지는 잘 못 잡아서, LLM이 목록을 보고 의미로 판단하게
// 바꿨다.
//
// 처음엔 LLM이 고른 이름을 문자열로 그대로 받았는데, "기도폐쇄"처럼 의미는 맞아도 목록의 정확한
// 표현("심폐소생술(이물질에 의한 기도폐쇄의 처치)")과 글자가 달라서 환각 방지 검증에 걸려 버려지는
// 경우가 실측으로 확인됐다. 그래서 이름 대신 번호를 고르게 한다 - 번호는 "미묘하게 다르게" 답할
// 여지가 없다.
@Slf4j
@Component
@RequiredArgsConstructor
class HealthTopicResolver {

    // FirstAidGuideService.MAX_ATTEMPTS와 같은 이유 - 예외/스키마 실패도 재시도 대상이다.
    // matchedIndex가 null인 것(관련 항목 없음 - HealthTopicPrompts 규칙 3)은 정상적인 최종 응답이라
    // 재시도 대상이 아니다.
    private static final int MAX_ATTEMPTS = 3;

    // FirstAidGuideService.guideCache와 달리 query는 자유 텍스트라 키 종류가 사실상 무한할 수 있어,
    // 크기를 제한한 LRU로 둔다 - 넘치면 가장 오래 안 쓰인 것부터 버린다.
    private static final int RESOLVE_CACHE_MAX_SIZE = 1000;

    private final ChatClient chatClient;

    @Value("${kdca.health-info.seed-file:classpath:seed.csv}")
    private Resource seedResource;

    private List<String> names = List.of();
    private String numberedListText = "";

    // query(원문 그대로) -> 매칭된 topic 캐시. 매 요청 483개 질병명 목록 전체를 LLM에 다시 넣는 게
    // 느리다는 지적(실측 확인)에 따라 추가했다. "매칭 없음"은 캐싱하지 않는다(FirstAidGuideService.
    // guideCache와 같은 이유 - 실패는 매번 다시 시도하는 편이 안전하다).
    // ponytail: LRU만 있고 TTL은 없다 - seed.csv가 재인덱싱으로 바뀌어도 앱을 재시작하기 전까지는
    // 옛 매칭이 남는다. 캐시 크기가 트래픽에 비해 부족해지면 Caffeine 같은 라이브러리로 교체.
    private final Map<String, String> resolveCache = Collections.synchronizedMap(
            new LinkedHashMap<>(RESOLVE_CACHE_MAX_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > RESOLVE_CACHE_MAX_SIZE;
                }
            });

    @PostConstruct
    void init() {
        try {
            names = readNames();
            numberedListText = IntStream.rangeClosed(1, names.size())
                    .mapToObj(i -> "%d. %s".formatted(i, names.get(i - 1)))
                    .collect(Collectors.joining("\n"));
            log.info("응급처치 상황 매칭용 질병명 목록 {}개를 불러왔습니다.", names.size());
        } catch (Exception e) {
            log.error("질병명 목록 로딩에 실패했습니다 - 응급처치 상황 매칭이 동작하지 않습니다.", e);
        }
    }

    // 자유 텍스트와 가장 가까운 질병/상황명을 찾는다. 목록 로딩에 실패했거나, LLM 호출이 실패했거나,
    // 관련 있는 항목이 없으면 빈 값을 돌려준다.
    Optional<String> resolve(String query) {
        if (names.isEmpty() || query == null || query.isBlank()) {
            return Optional.empty();
        }

        String cached = resolveCache.get(query);
        if (cached != null) {
            return Optional.of(cached);
        }

        RawTopicMatch raw = resolveWithRetry(query);
        if (raw == null || raw.matchedIndex() == null) {
            return Optional.empty();
        }
        int index = raw.matchedIndex();
        // LLM이 목록 범위를 벗어난 번호를 지어내는 경우(환각)를 막는다.
        if (index < 1 || index > names.size()) {
            log.warn("LLM이 목록 범위를 벗어난 번호를 반환했습니다. query=\"{}\", matchedIndex={}", query, index);
            return Optional.empty();
        }
        String matched = names.get(index - 1);
        resolveCache.put(query, matched);
        return Optional.of(matched);
    }

    // 예외(타임아웃/429/네트워크 오류)나 raw 자체가 null(스키마 파싱 실패)일 때만 재시도한다.
    // matchedIndex가 null인 것(관련 항목 없음)은 여기서 raw를 그대로 반환해 즉시 최종 응답으로 쓴다 -
    // 정상적인 "매칭 없음"까지 재시도하면 없는 걸 억지로 골라낼 위험이 있다.
    private RawTopicMatch resolveWithRetry(String query) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RawTopicMatch raw;
            try {
                raw = chatClient.prompt()
                        .system(HealthTopicPrompts.RESOLVE_PROMPT)
                        .user("[목록]\n%s\n\n[상황]\n%s".formatted(numberedListText, query))
                        .call()
                        .entity(RawTopicMatch.class);
            } catch (Exception e) {
                log.warn("상황-질병명 매칭 LLM 호출이 실패했습니다 (시도 {}/{}). query=\"{}\"", attempt, MAX_ATTEMPTS, query, e);
                continue;
            }
            if (raw != null) {
                return raw;
            }
            log.warn("상황-질병명 매칭 LLM 구조화 출력이 예상 스키마를 벗어났습니다 (시도 {}/{}). query=\"{}\"",
                    attempt, MAX_ATTEMPTS, query);
        }
        return null;
    }

    // seed.csv는 질병명에 콤마가 포함된 행이 있어(예: "굴절이상(근시, 원시, 난시)") 정식 CSV 파서로
    // 읽는다 - HealthInfoIndexingRunner.readSeed와 같은 이유다. 그쪽은 index 프로파일에서만 뜨는
    // 컴포넌트라 기본 구동 시 주입받을 수 없어 여기서 따로 읽는다.
    private List<String> readNames() throws IOException {
        try (Reader reader = new InputStreamReader(seedResource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {

            List<String> result = new ArrayList<>();
            for (CSVRecord record : parser) {
                String disease = record.get("disease");
                if (disease != null && !disease.isBlank()) {
                    result.add(disease.trim());
                }
            }
            return result;
        }
    }
}
