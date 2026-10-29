package com.tadaktadak.eunggeubi.domain.first_aid.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.service.LlmStreaming;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.RagRetrievalService;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidGuideResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidSearchResponse;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.FirstAidTopic;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawFirstAidGuide;
import com.tadaktadak.eunggeubi.domain.first_aid.dto.RawStepOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

// health_info 컬렉션에서 검색한 문서를 근거로, 그 자리에서 LLM이 응급처치 단계를 생성한다
// (AiConsultationService의 1차 답변 생성과 같은 RAG 패턴). topic별로 생성 결과를 캐싱한다
// (guideCache 참고) - 매 요청 LLM을 다시 부르면 느려서, 최초 1회만 생성하고 재사용한다.
//
// 4개 상황(화상/코피/골절/기도막힘)으로 한정하지 않는다 - HealthTopicResolver가 health_info 컬렉션에
// 있는 어떤 질병/상황명이든 자유 텍스트와 의미로 매칭해준다. 매칭된 이후의 본문 검색은 유사도가 아니라
// disease 메타데이터 필터로 정확히 그 질병의 청크만 가져온다(RagRetrievalService.retrieveByExactDisease) -
// "골절"처럼 검색어 자체가 다른 질병의 "치료" 섹션과 더 가깝게 임베딩되어 전혀 무관한 내용이 섞이는
// 문제(실측 확인)를 막기 위함이다.
@Slf4j
@Service
@RequiredArgsConstructor
public class FirstAidGuideService {

    // 짧은 프롬프트(응급처치 생성)에서도 체크리스트 생성과 마찬가지로 LLM이 스키마를 벗어난 응답을
    // 주는 경우가 있어 몇 번 재시도한다 - 실패하면 기본값으로 대충 채우기보다는 호출자가 오류로
    // 처리하게 한다(guideFor가 실패는 캐싱하지 않으므로 다음 요청에서 다시 시도된다).
    private static final int MAX_ATTEMPTS = 3;

    // 전원 차단·화재 진압·유해물질 격리처럼 "지금도 계속 위험을 주는 요인을 없애는 조치"가 담긴 내용을
    // 참고자료 맨 앞으로 옮긴다 - FirstAidPrompts 규칙 6으로 "참고자료 순서와 무관하게 맨 앞으로 옮겨라"라고
    // 지시해도, 모델이 참고자료 원문 순서를 그대로 따라가는 경향이 강해서 실제로는 안 바뀌는 경우가
    // 실측으로 확인됐다(예: 화상 - "전기 스위치 차단"이 지시를 줘도 항상 맨 뒤에 남음). 입력 순서 자체를
    // 바꿔서 모델의 "입력 순서를 따라간다"는 성향을 거꾸로 활용한다.
    //
    // 저체온증의 "체온 소실을 막는다"류 문구도 같은 성격이라 이 패턴에 추가해봤지만, 실측 결과 오히려
    // 역효과였다 - 관련 문서 2개가 앞으로 몰리자 모델이 "이걸로 충분하다"고 판단해 뒤쪽의 심각도별
    // 세부 처치(34도/33도/32도/31도~28도) 내용을 통째로 생략해버렸다(steps가 6~7개에서 2개로 줄어듦).
    // 순서는 대부분 프롬프트 지시만으로도 이미 맞게 나와서, 완성도를 해치면서까지 재배열할 실익이 없어
    // 저체온증은 여기 포함하지 않는다 - 문서 재배열은 "완전히 안 바뀌는" 화상 케이스처럼 꼭 필요할 때만.
    private static final Pattern HAZARD_REMOVAL_PATTERN = Pattern.compile(
            "전원을?\\s*(차단|끄)|스위치를?\\s*(내려|끄)|전기\\s*(공급|스위치)를?\\s*차단"
                    + "|화재를?\\s*(진압|끄)|불을?\\s*끄"
                    + "|가스\\s*(누출|밸브)|누출된?\\s*가스"
                    + "|유해물질(로부터|을)|위험\\s*물질(로부터|을)"
    );

    // 화상의 "치료" 섹션처럼 화학/흡입/전기 화상 설명이 문단 구분(개행)만 있고 하나의 문서로 통째로
    // 인덱싱된 경우, 문서 단위 재배열(prioritizeHazardRemoval)만으로는 문서 "안"의 순서를 못 바꾼다
    // (실측 확인) - 그래서 문서 텍스트 자체도 이 구분자로 문단을 나눠 문단 단위로 재배열한다.
    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\n+");

    private final RagRetrievalService ragRetrievalService;
    private final ChatClient chatClient;
    private final HealthTopicResolver healthTopicResolver;

    // topic(질병명)별로 생성된 가이드를 캐싱한다 - 참고자료가 안 바뀌는 한 같은 topic은 항상 같은
    // 내용이어야 하는데, 매 요청 LLM을 다시 불러서(재시도까지 겹치면 최대 5초 가까이) 응답이 느리다는
    // 지적(실측 확인)이 있었다. topic 후보가 483개로 유한해서 무한정 커질 걱정은 없다.
    // ponytail: 재인덱싱(HealthInfoIndexingRunner)해도 이 캐시는 안 지워진다 - 앱을 재시작하기 전까지
    // 옛 내용이 남는다. 재인덱싱 직후 캐시를 비우는 관리용 엔드포인트가 필요해지면 그때 추가한다.
    private final Map<String, Guide> guideCache = new ConcurrentHashMap<>();

    // AiConsultationService/ConsultationRegenerationService가 상담 원문에 응급처치로 안내할 만한
    // 상황이 있는지 확인할 때 쓴다 - 실제 가이드 생성 없이 상황명만 필요할 때.
    public Optional<String> matchKnownSituation(String text) {
        return healthTopicResolver.resolve(text);
    }

    public FirstAidGuideResponse getBySituation(String situation) {
        return getBySituation(situation, null);
    }

    // caseName은 세부 상황(FirstAidTopic.cases)이 있는 주제에서 사용자가 고른 것. 세부 상황이 있는데 아직 안
    // 골랐으면 가이드를 만들지 않고 고를 목록만 돌려준다 - 개·뱀·벌 처치를 한 목록에 섞으면 서로 다른 처치가
    // 번호까지 붙어 한 순서처럼 보였다(실측). 목록에 없는 값은 거부한다 - 요청 값이 그대로 프롬프트와 캐시
    // 키에 들어가기 때문이다.
    public FirstAidGuideResponse getBySituation(String situation, String caseName) {
        return getBySituation(situation, caseName, null);
    }

    // onPartial이 있으면 처음 만드는 가이드를 스트리밍으로 생성하면서, 완성된 step이 늘 때마다 지금까지의
    // 가이드를 넘긴다(ordered는 아직 모르니 false). 재시도하면 빈 steps로 한 번 지운다. 캐시에 있으면 안 부른다.
    // 반환값은 스트리밍이 아닐 때와 같은 최종 응답이다 - 번호(ordered)·제목은 이걸로 확정된다.
    public FirstAidGuideResponse getBySituation(String situation, String caseName,
                                                Consumer<FirstAidGuideResponse> onPartial) {
        String resolvedTopic = healthTopicResolver.resolve(situation)
                .orElseThrow(() -> new IllegalArgumentException("해당 상황의 응급처치 정보를 찾을 수 없습니다."));

        Optional<FirstAidTopic> info = healthTopicResolver.findTopic(resolvedTopic);
        List<String> cases = info.map(FirstAidTopic::cases).orElse(List.of());
        if (caseName == null && !cases.isEmpty()) {
            String title = info.get().label() + " 응급처치";
            return FirstAidGuideResponse.chooseCase(situation, title, cases);
        }
        if (caseName != null && !cases.contains(caseName)) {
            throw new IllegalArgumentException("해당 상황의 응급처치 정보를 찾을 수 없습니다.");
        }

        // 생성 중 제목: 목록에 없는 주제는 최종 제목이 LLM이 지은 것이라 끝날 때 바뀔 수 있다.
        String partialTitle = caseName != null
                ? caseName + " 응급처치"
                : titleFor(resolvedTopic, resolvedTopic + " 응급처치");
        Consumer<List<String>> onSteps = onPartial == null ? null
                : steps -> onPartial.accept(new FirstAidGuideResponse(situation, partialTitle, steps, false, List.of()));
        Guide guide = guideFor(resolvedTopic, caseName, onSteps)
                .orElseThrow(() -> new IllegalArgumentException("해당 상황의 응급처치 정보를 찾을 수 없습니다."));
        // situation은 호출자가 준 원래 표현(예: 프론트 칩 라벨 "기도막힘")을 그대로 돌려준다 - 내부적으로만
        // resolvedTopic(코퍼스 문서명, 예: "심폐소생술(이물질에 의한 기도폐쇄의 처치)")으로 검색한다.
        String title = caseName != null ? caseName + " 응급처치" : titleFor(resolvedTopic, guide.raw().title());
        return FirstAidGuideResponse.of(situation, title, guide.raw(), guide.ordered());
    }

    // 자유 텍스트로 health_info 컬렉션을 검색해서, 현장 응급처치로 다룰 만한 내용이 있으면 그 자리에서
    // 가이드를 생성한다.
    public Optional<FirstAidGuideResponse> search(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Optional<String> resolvedTopic = healthTopicResolver.resolve(text);
        if (resolvedTopic.isEmpty()) {
            return Optional.empty();
        }

        // 세부 상황이 있는 주제는 섞인 가이드를 만들지 않고 고를 목록만 준다 - 호출부(재생성 배너)는
        // situation·title만 쓰고, 배너를 누르면 응급처치 화면이 getBySituation으로 세부 상황을 고르게 한다.
        Optional<FirstAidTopic> info = healthTopicResolver.findTopic(resolvedTopic.get());
        if (info.isPresent() && !info.get().cases().isEmpty()) {
            return Optional.of(FirstAidGuideResponse.chooseCase(
                    resolvedTopic.get(), info.get().label() + " 응급처치", info.get().cases()));
        }

        return guideFor(resolvedTopic.get(), null, null)
                .map(guide -> FirstAidGuideResponse.of(
                        resolvedTopic.get(), titleFor(resolvedTopic.get(), guide.raw().title()), guide.raw(), guide.ordered()));
    }

    // 화면 제목은 검색 후보 카드와 같은 이름으로 맞춘다("베인 상처 (열상) 응급처치") - 카드를 누르고 들어온
    // 사람이 같은 이름을 보게 한다. 응급처치 검색 목록에 없는 주제는 fallback(LLM이 지은 제목)을 그대로 쓴다.
    private String titleFor(String topic, String fallback) {
        return healthTopicResolver.findTopic(topic)
                .map(info -> info.label() + " 응급처치")
                .orElse(fallback);
    }

    // 응급처치 검색창 전용. 가이드는 만들지 않고 후보 이름만 돌려준다 - 사용자가 고른 것만
    // getBySituation으로 생성한다(후보마다 다 만들면 처음 보는 주제는 하나당 몇 초씩 걸려서 너무 느리다).
    // 후보는 가이드가 실제로 만들어지는 주제만 모은 목록(first-aid-topics.txt)에서 고르므로 여기서 다시
    // 가이드를 만들어 걸러내지 않는다 - 그렇게 하던 땐 검색이 37초까지 걸렸다. LLM 편차로 드물게 가이드
    // 생성이 실패하면 프론트가 그 후보를 목록에서 빼고 안내한다.
    public FirstAidSearchResponse searchCandidates(String text) {
        return new FirstAidSearchResponse(healthTopicResolver.resolveCandidates(text).stream()
                .map(healthTopicResolver::describe)
                .toList());
    }

    // 응급처치 화면의 "상황별로 찾기" 목록. 검색 후보와 같은 카드 정보라 응답 모양도 같다.
    public FirstAidSearchResponse listTopics() {
        return new FirstAidSearchResponse(healthTopicResolver.allTopics());
    }

    // 캐시에 있으면 그대로 쓰고, 없으면 검색+생성 후 성공한 결과만 캐싱한다. 실패(참고자료 없음/현장
    // 조치 없음)는 캐싱하지 않는다 - 실패 원인(seed.csv 갱신, 재인덱싱)이 나중에 해소될 수 있어서
    // 매번 다시 시도하는 편이 안전하다.
    // caseName이 있으면 같은 참고자료로 그 세부 상황의 처치만 만들고, 캐시도 세부 상황별로 따로 둔다.
    // onSteps는 새로 생성할 때만 불린다(generateWithRetry 참고).
    private Optional<Guide> guideFor(String topic, String caseName, Consumer<List<String>> onSteps) {
        String situation = caseName == null ? topic : "%s - %s".formatted(topic, caseName);
        Guide cached = guideCache.get(situation);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<Document> docs = ragRetrievalService.retrieveByExactDisease(topic);
        if (docs.isEmpty()) {
            return Optional.empty();
        }

        return generateWithRetry(situation, docs, onSteps).map(raw -> {
            Guide guide = new Guide(raw, classifyOrdered(situation, raw.steps()));
            guideCache.put(situation, guide);
            return guide;
        });
    }

    // 만들어진 steps를 순서대로 해야 하는지 따로 묻는다(RawStepOrder 참고). 한 단계뿐이면 순서라는 게
    // 없어 LLM을 부르지 않는다. 실패하면 null(=번호 없음)로 둔다.
    // ponytail: 판단 실패도 가이드와 함께 캐싱돼서 재시작 전까지 번호 없이 보인다 - 실패가 잦아지면
    // 재시도를 붙이거나 판단 결과만 따로 캐싱한다.
    private Boolean classifyOrdered(String topic, List<String> steps) {
        if (steps.size() <= 1) {
            return false;
        }
        String numbered = IntStream.rangeClosed(1, steps.size())
                .mapToObj(i -> "%d. %s".formatted(i, steps.get(i - 1)))
                .collect(Collectors.joining("\n"));
        try {
            RawStepOrder raw = chatClient.prompt()
                    .system(FirstAidPrompts.STEP_ORDER_PROMPT)
                    .user("[상황]\n%s\n\n[응급처치 단계]\n%s".formatted(topic, numbered))
                    .call()
                    .entity(RawStepOrder.class);
            return raw == null ? null : raw.ordered();
        } catch (Exception e) {
            log.warn("응급처치 순서 판단 LLM 호출이 실패했습니다. topic=\"{}\"", topic, e);
            return null;
        }
    }

    private record Guide(RawFirstAidGuide raw, Boolean ordered) {
    }

    // steps가 빈 배열인 응답(FirstAidPrompts 규칙 4 - 참고자료에 현장 조치·병원 안내가 없다는 뜻)도 재시도
    // 대상으로 묶는다 - 스키마상 정상 응답이지만, 같은 topic·같은 참고자료로도 한 번은 steps가 나오고
    // 한 번은 빈 배열이 나오는 경우가 실측으로 확인됐다(LLM 샘플링 편차). 그래서 여기서 최종적으로
    // Optional이 채워져 돌아오면 steps가 비어있지 않음을 호출부(getBySituation/search)가 보장받는다 -
    // MAX_ATTEMPTS를 다 써도 계속 비어있으면 그때는 정말 현장 조치가 없는 것으로 보고 empty를 반환한다.
    private Optional<RawFirstAidGuide> generateWithRetry(String topic, List<Document> docs,
                                                         Consumer<List<String>> onSteps) {
        String userPrompt = "[상황]\n%s\n\n[참고자료]\n%s"
                .formatted(topic, ragRetrievalService.buildContext(prioritizeHazardRemoval(docs)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RawFirstAidGuide raw;
            try {
                if (onSteps == null) {
                    raw = chatClient.prompt()
                            .system(FirstAidPrompts.FIRST_AID_PROMPT)
                            .user(userPrompt)
                            .call()
                            .entity(RawFirstAidGuide.class);
                } else {
                    if (attempt > 1) {
                        onSteps.accept(List.of()); // 실패한 시도에서 보낸 step은 지운다
                    }
                    raw = streamGuide(userPrompt, onSteps);
                }
            } catch (Exception e) {
                log.warn("응급처치 가이드 LLM 호출이 실패했습니다 (시도 {}/{}). topic=\"{}\"", attempt, MAX_ATTEMPTS, topic, e);
                continue;
            }

            if (raw == null || raw.title() == null || raw.steps() == null) {
                log.warn("응급처치 가이드 LLM 구조화 출력이 예상 스키마를 벗어났습니다 (시도 {}/{}). topic=\"{}\", raw={}",
                        attempt, MAX_ATTEMPTS, topic, raw);
                continue;
            }
            if (raw.steps().isEmpty()) {
                log.warn("응급처치 가이드 LLM이 steps를 빈 배열로 응답했습니다 (시도 {}/{}). topic=\"{}\"",
                        attempt, MAX_ATTEMPTS, topic);
                continue;
            }
            return Optional.of(raw);
        }
        return Optional.empty();
    }

    // 받는 중에 닫힌 steps 원소(=완성된 step)가 늘 때마다 지금까지의 steps를 넘긴다.
    private RawFirstAidGuide streamGuide(String userPrompt, Consumer<List<String>> onSteps) {
        int[] sent = {0};
        return LlmStreaming.stream(chatClient, FirstAidPrompts.FIRST_AID_PROMPT, userPrompt, RawFirstAidGuide.class,
                buffer -> {
                    List<String> steps = LlmStreaming.closedArrayItems(buffer, "steps");
                    if (steps.size() > sent[0]) {
                        sent[0] = steps.size();
                        onSteps.accept(steps);
                    }
                });
    }

    // RawFirstAidGuide는 sourceIndexes를 안 쓰므로(title/steps만), 순서를 바꿔도 인용 번호가 깨질 일이
    // 없다 - 안전하게 재배열할 수 있다. 안정 정렬(Stream.sorted)이라 위험 제거 문단/문서들 사이의, 그리고
    // 나머지 문단/문서들 사이의 상대 순서는 그대로 유지된다. 문서 안의 문단부터 먼저 재배열한 뒤
    // (prioritizeHazardParagraphs), 문서 자체도 재배열한다 - 위험 제거 내용이 별도 문서로 검색된 경우도
    // 있을 수 있어서 둘 다 필요하다.
    private List<Document> prioritizeHazardRemoval(List<Document> docs) {
        return docs.stream()
                .map(this::prioritizeHazardParagraphs)
                .sorted(Comparator.comparing(doc -> HAZARD_REMOVAL_PATTERN.matcher(doc.getText()).find() ? 0 : 1))
                .toList();
    }

    private Document prioritizeHazardParagraphs(Document doc) {
        String[] paragraphs = PARAGRAPH_SPLIT.split(doc.getText());
        if (paragraphs.length <= 1) {
            return doc;
        }
        String reordered = Arrays.stream(paragraphs)
                .sorted(Comparator.comparing(p -> HAZARD_REMOVAL_PATTERN.matcher(p).find() ? 0 : 1))
                .collect(Collectors.joining("\n"));
        return new Document(reordered, doc.getMetadata());
    }
}
