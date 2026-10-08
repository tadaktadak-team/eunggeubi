package com.tadaktadak.eunggeubi.domain.first_aid.service;

import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidTopic;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawRelevanceCheck;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicCandidates;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawTopicMatch;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
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

    private static final int MAX_CANDIDATES = 5;
    // 관련성 확인이 후보를 다 지웠을 때 대신 보여줄 1차 후보 수 - 1차는 관련 높은 순이라 앞쪽만 쓴다.
    private static final int FALLBACK_CANDIDATES = 3;

    private final ChatClient chatClient;

    @Value("${kdca.health-info.seed-file:classpath:seed.csv}")
    private Resource seedResource;

    private List<String> names = List.of();
    private String numberedListText = "";

    // 응급처치 검색창(resolveCandidates)은 483개 전체가 아니라 현장 응급처치가 실제로 만들어지는 주제만
    // 모은 목록에서 고른다. 전체 목록에서 고르게 하면 대부분이 당뇨병·관절염처럼 응급처치가 없는 주제라,
    // 엉뚱한 후보("눈"에 폐색전증)가 섞이거나 맞는 주제가 있어도 0개("코"에 코피가 안 나옴)가 나오고,
    // 응급처치 없는 후보를 걸러내느라 검색이 37초까지 걸렸다(실측). 목록은 483개 주제마다 가이드 생성을
    // 실제로 돌려 성공한 것만 모았다 - seed.csv가 바뀌면 다시 만들어야 한다.
    @Value("${kdca.health-info.first-aid-topics-file:classpath:first-aid-topics.txt}")
    private Resource firstAidTopicsResource;

    private List<String> firstAidNames = List.of();
    // 주제 이름 -> 카드 표시 정보(짧은 이름·분류·설명). 목록 파일 순서를 유지한다.
    private Map<String, FirstAidTopic> firstAidTopics = Map.of();

    // 응급처치 검색(후보 고르기·관련성 확인)에만 쓰는 모델. 앱 기본(gpt-4o-mini)으로는 "손가락"에 골절·열상이
    // 목록에 있는데도 "고를 게 없다"(matches=null)고 답하거나, "손"에 치아외상을 넣는 판단 실수가 프롬프트를
    // 고쳐도 남았다(실측). 검색어별로 결과를 캐싱하므로 비싼 모델은 새 검색어에만 호출된다.
    @Value("${kdca.first-aid.search-model:gpt-4.1}")
    private String searchModel;
    private String numberedFirstAidText = "";

    // query(원문 그대로) -> 매칭된 topic 캐시. 매 요청 483개 질병명 목록 전체를 LLM에 다시 넣는 게
    // 느리다는 지적(실측 확인)에 따라 추가했다. "매칭 없음"은 캐싱하지 않는다(FirstAidGuideService.
    // guideCache와 같은 이유 - 실패는 매번 다시 시도하는 편이 안전하다).
    // ponytail: LRU만 있고 TTL은 없다 - seed.csv가 재인덱싱으로 바뀌어도 앱을 재시작하기 전까지는
    // 옛 매칭이 남는다. 캐시 크기가 트래픽에 비해 부족해지면 Caffeine 같은 라이브러리로 교체.
    private final Map<String, String> resolveCache = lruCache();
    // resolveCandidates용 - resolveCache와 같은 정책(매칭 없음은 캐싱하지 않음).
    private final Map<String, List<String>> candidatesCache = lruCache();

    private static <V> Map<String, V> lruCache() {
        return Collections.synchronizedMap(new LinkedHashMap<>(RESOLVE_CACHE_MAX_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
                return size() > RESOLVE_CACHE_MAX_SIZE;
            }
        });
    }

    @PostConstruct
    void init() {
        try {
            names = readNames();
            numberedListText = numbered(names);
            log.info("응급처치 상황 매칭용 질병명 목록 {}개를 불러왔습니다.", names.size());
        } catch (Exception e) {
            log.error("질병명 목록 로딩에 실패했습니다 - 응급처치 상황 매칭이 동작하지 않습니다.", e);
        }
        try {
            firstAidTopics = readFirstAidTopics();
            firstAidNames = List.copyOf(firstAidTopics.keySet());
            numberedFirstAidText = numbered(firstAidNames);
            log.info("응급처치 검색용 주제 목록 {}개를 불러왔습니다. 검색 모델={}", firstAidNames.size(), searchModel);
        } catch (Exception e) {
            // 목록이 없으면 전체 목록으로 검색한다 - 정확도는 떨어져도 검색 자체는 동작한다.
            log.error("응급처치 검색용 주제 목록 로딩에 실패해 전체 목록으로 검색합니다.", e);
        }
        if (firstAidNames.isEmpty()) {
            firstAidNames = names;
            numberedFirstAidText = numberedListText;
        }
    }

    private static String numbered(List<String> list) {
        return IntStream.rangeClosed(1, list.size())
                .mapToObj(i -> "%d. %s".formatted(i, list.get(i - 1)))
                .collect(Collectors.joining("\n"));
    }

    // 자유 텍스트와 가장 가까운 질병/상황명을 찾는다. 목록 로딩에 실패했거나, LLM 호출이 실패했거나,
    // 관련 있는 항목이 없으면 빈 값을 돌려준다.
    Optional<String> resolve(String query) {
        if (names.isEmpty() || query == null || query.isBlank()) {
            return Optional.empty();
        }
        // 응급처치 검색 후보(resolveCandidates)를 눌러 들어오면 목록의 이름이 그대로 온다 - LLM을 거칠 필요가 없다.
        if (names.contains(query)) {
            return Optional.of(query);
        }

        String cached = resolveCache.get(query);
        if (cached != null) {
            return Optional.of(cached);
        }

        RawTopicMatch raw = callWithRetry(
                HealthTopicPrompts.RESOLVE_PROMPT, query, numberedListText, null, RawTopicMatch.class);
        if (raw == null) {
            return Optional.empty();
        }
        Optional<String> matched = verify(query, raw.matchedIndex(), raw.matchedName(), names);
        matched.ifPresent(name -> resolveCache.put(query, name));
        return matched;
    }

    // 응급처치 검색창 전용 - 검색어가 애매하면 사용자가 고를 수 있게 후보를 최대 5개까지 돌려준다
    // (분명하면 1개). AI 상담의 "응급처치 바로보기"처럼 하나만 필요한 곳은 resolve()를 쓴다.
    List<String> resolveCandidates(String query) {
        if (names.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }
        if (names.contains(query)) {
            return List.of(query);
        }

        List<String> cached = candidatesCache.get(query);
        if (cached != null) {
            return cached;
        }

        List<String> matched = pickCandidates(query);
        // 같은 검색어도 실행마다 결과가 달라서("눈"이 한 번은 안외상 5개, 한 번은 0개 - 실측, 경고 없이
        // LLM이 빈 배열을 줌), 짧은 응급 목록에서 0개가 나오면 한 번 더 묻는다. 정말 관련 없는 검색어
        // ("주식")면 두 번째도 0개라 그대로 0개다.
        if (matched.isEmpty()) {
            matched = pickCandidates(query);
        }
        List<String> relevant = keepRelevant(query, matched);
        // 관련성 확인이 후보를 전부 지우면 1차 결과 앞쪽을 대신 보여준다 - 응급 검색에선 0개가 약간 부정확한
        // 후보보다 나쁘다("아파요"·"토했어요"·"입"이 확인 단계에서 다 지워져 0개가 됐다 - 실측). 1차부터 0개인
        // 검색어("주식")는 그대로 0개다.
        if (relevant.isEmpty() && !matched.isEmpty()) {
            relevant = matched.subList(0, Math.min(FALLBACK_CANDIDATES, matched.size()));
        }
        log.info("응급처치 검색 후보. query=\"{}\", 고른 항목={}, 최종={}", query, matched, relevant);
        if (!relevant.isEmpty()) {
            candidatesCache.put(query, relevant);
        }
        return relevant;
    }

    private List<String> pickCandidates(String query) {
        RawTopicCandidates raw = callWithRetry(
                HealthTopicPrompts.CANDIDATES_PROMPT, query, numberedFirstAidText, searchOptions(), RawTopicCandidates.class);
        log.info("응급처치 후보 LLM 원본 응답. model={}, query=\"{}\", raw={}", searchModel, query, raw);
        if (raw == null || raw.matches() == null) {
            return List.of();
        }
        return raw.matches().stream()
                .filter(Objects::nonNull)
                .flatMap(pick -> verify(query, pick.index(), pick.name(), firstAidNames).stream())
                .distinct()
                .limit(MAX_CANDIDATES)
                .toList();
    }

    // 483개 목록을 보면서 고르면 "손"에 치아손상(이름의 '손상')·"발"에 무릎처럼 엉뚱한 항목이 섞이는 게
    // 프롬프트 지시만으로는 안 막혔다(실측). 고른 후보만 짧게 다시 보여주고 [상황]과 정말 관련 있는지만
    // 따로 묻는다 - 응급처치 순서 판단(FirstAidGuideService.classifyOrdered)을 따로 묻자 정확도가 오른
    // 것과 같은 방식이다. 확인 호출이 실패하면 후보를 잃지 않게 원래 후보를 그대로 쓴다.
    private List<String> keepRelevant(String query, List<String> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        String numbered = IntStream.rangeClosed(1, candidates.size())
                .mapToObj(i -> "%d. %s".formatted(i, candidates.get(i - 1)))
                .collect(Collectors.joining("\n"));
        RawRelevanceCheck raw;
        try {
            raw = chatClient.prompt()
                    .options(searchOptions())
                    .system(HealthTopicPrompts.RELEVANCE_CHECK_PROMPT)
                    .user("[상황]\n%s\n\n[후보]\n%s".formatted(query, numbered))
                    .call()
                    .entity(RawRelevanceCheck.class);
        } catch (Exception e) {
            log.warn("응급처치 후보 관련성 확인 LLM 호출이 실패해 후보를 그대로 씁니다. query=\"{}\"", query, e);
            return candidates;
        }
        if (raw == null || raw.items() == null) {
            return candidates;
        }
        log.info("응급처치 후보 관련성 판단. query=\"{}\", candidates={}, items={}", query, candidates, raw.items());
        // 판단이 빠진 후보(items에 없음/relevant=null)는 남긴다 - 확인 실패로 후보를 잃지 않게.
        Set<Integer> dropped = raw.items().stream()
                .filter(item -> item != null && item.index() != null && Boolean.FALSE.equals(item.relevant()))
                .map(RawRelevanceCheck.Item::index)
                .collect(Collectors.toSet());
        return IntStream.rangeClosed(1, candidates.size())
                .filter(i -> !dropped.contains(i))
                .mapToObj(i -> candidates.get(i - 1))
                .toList();
    }

    // LLM이 준 번호와 이름을 서로 맞춰본다 - 번호만 믿으면 옆 번호를 잘못 적는 실수(화상 38 대신 황달 39)를,
    // 이름만 믿으면 줄여 쓴 이름("기도폐쇄")을 못 잡는다. 순서대로:
    // 1) 이름이 목록에 그대로 있으면 번호와 상관없이 그 이름 (번호 실수 교정)
    // 2) 번호 자리의 이름이 LLM이 준 이름을 포함하면 그 항목 (줄여 쓴 이름)
    // 3) LLM이 준 이름을 포함하는 항목이 목록에 딱 하나면 그 항목 (번호도 틀리고 이름도 줄여 쓴 경우)
    // 셋 다 아니면 어느 쪽도 믿을 수 없어 버린다.
    private Optional<String> verify(String query, Integer index, String name, List<String> names) {
        if (name == null || name.isBlank()) {
            // 이름을 빠뜨리면 번호만이라도 쓴다 - 예전엔 경고 없이 버려져 "왜 0개인지" 로그로도 안 보였다.
            if (index != null && index >= 1 && index <= names.size()) {
                log.warn("LLM이 이름 없이 번호만 줘서 번호로 고릅니다. query=\"{}\", index={}", query, index);
                return Optional.of(names.get(index - 1));
            }
            return Optional.empty();
        }
        String trimmed = name.trim();
        if (names.contains(trimmed)) {
            return Optional.of(trimmed);
        }
        if (index != null && index >= 1 && index <= names.size() && names.get(index - 1).contains(trimmed)) {
            return Optional.of(names.get(index - 1));
        }
        List<String> containing = names.stream().filter(n -> n.contains(trimmed)).limit(2).toList();
        if (containing.size() == 1) {
            return Optional.of(containing.get(0));
        }
        log.warn("LLM이 준 번호와 이름이 목록과 맞지 않아 버립니다. query=\"{}\", index={}, name=\"{}\"", query, index, name);
        return Optional.empty();
    }

    // 예외(타임아웃/429/네트워크 오류)나 raw 자체가 null(스키마 파싱 실패)일 때만 재시도한다.
    // matchedIndex가 null인 것(관련 항목 없음)은 여기서 raw를 그대로 반환해 즉시 최종 응답으로 쓴다 -
    // 정상적인 "매칭 없음"까지 재시도하면 없는 걸 억지로 골라낼 위험이 있다.
    private OpenAiChatOptions searchOptions() {
        return OpenAiChatOptions.builder().model(searchModel).temperature(0.2).build();
    }

    // options가 null이면 앱 기본 모델(application.yml)을 쓴다.
    private <T> T callWithRetry(String systemPrompt, String query, String numberedList,
                                OpenAiChatOptions options, Class<T> type) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            T raw;
            try {
                ChatClient.ChatClientRequestSpec spec = chatClient.prompt();
                if (options != null) {
                    spec = spec.options(options);
                }
                raw = spec
                        .system(systemPrompt)
                        .user("[목록]\n%s\n\n[상황]\n%s".formatted(numberedList, query))
                        .call()
                        .entity(type);
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

    // 한 줄에 주제 하나. 빈 줄과 #으로 시작하는 줄은 건너뛴다. seed.csv에 없는 이름은 가이드를 만들 수
    // 없으니 버린다(오타·옛 목록 방어).
    // 검색 후보 카드에 보여줄 정보. 목록에 없는 주제(전체 목록으로 검색한 경우)는 원래 이름만 보여준다.
    FirstAidTopic describe(String topic) {
        return firstAidTopics.getOrDefault(topic, FirstAidTopic.bare(topic));
    }

    // first-aid-topics.txt 형식: "[분류]" 줄 아래에 "주제 | 짧은 이름 | 설명". 짧은 이름·설명은 생략할 수 있다.
    // 빈 줄과 #으로 시작하는 줄은 건너뛰고, seed.csv에 없는 주제는 가이드를 만들 수 없으니 버린다.
    private Map<String, FirstAidTopic> readFirstAidTopics() throws IOException {
        Map<String, FirstAidTopic> result = new LinkedHashMap<>();
        String category = "";
        try (var reader = new BufferedReader(
                new InputStreamReader(firstAidTopicsResource.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line : (Iterable<String>) reader.lines()::iterator) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    category = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                String topic = parts[0].trim();
                if (!names.contains(topic)) {
                    log.warn("응급처치 검색 목록의 주제가 seed.csv에 없어 버립니다: \"{}\"", topic);
                    continue;
                }
                String shortName = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : topic;
                String description = parts.length > 2 ? parts[2].trim() : "";
                result.putIfAbsent(topic, new FirstAidTopic(topic, shortName, category, description));
            }
        }
        return result;
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
