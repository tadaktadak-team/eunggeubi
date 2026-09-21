package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawAnswer;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ReferenceSourceRepository;
import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

// health_info 컬렉션에서 관련 문서를 검색한 뒤, 그 문서만 근거로 LLM 답변을 생성한다(RAG).
// 답변은 문장(segment) 단위로 쪼개고 각 문장이 인용한 참고자료 번호를 함께 받아서,
// 프론트에서 문장별로 출처를 붙일 수 있게 한다.
// 대화 한 턴(사용자 메시지 + AI 응답)을 ai_consultations/reference_sources에 저장한다 - 이후 체크리스트
// 생성/재생성(ChecklistService, ConsultationRegenerationService)이 이 저장된 상담을 이어서 쓴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConsultationService {

    // 모델이 119 안내 규칙(AiConsultationPrompts.CONSULT_PROMPT 6번)을 놓쳐도, 사용자 입력에 명백한
    // 응급 징후(EmergencySignalGuard)가 있으면 이 문장을 코드가 강제로 답변 맨 앞에 붙인다.
    private static final ConsultationResponse.AnswerSegment FORCED_EMERGENCY_NOTICE =
            new ConsultationResponse.AnswerSegment("지금 말씀하신 증상은 응급 상황일 수 있습니다. 즉시 119에 신고해주세요.", List.of());

    // 후속 질문에 맥락을 얼마나 태울지 - 프롬프트 길이/비용을 생각해서 최근 몇 개(=몇 턴)로 제한한다.
    private static final int MAX_HISTORY_MESSAGES = 6;

    // ChecklistService.generateItemsWithRetry와 같은 이유 - 타임아웃/429 같은 호출 자체 실패도
    // 재시도 대상이다. 안 잡으면 일시적인 오류 한 번에 전체 요청이 500으로 끝난다.
    private static final int MAX_ATTEMPTS = 3;

    private final RagRetrievalService ragRetrievalService;
    private final ChatClient chatClient;
    private final AiConsultationRepository aiConsultationRepository;
    private final ReferenceSourceRepository referenceSourceRepository;
    private final HealthProfileRepository healthProfileRepository;

    // ponytail: 이 메서드 전체를 트랜잭션으로 감싸지 않는다 - RAG 검색+LLM 호출이 초 단위로 걸리는데
    // 그동안 DB 커넥션을 잡고 있으면 동시 요청 몇 개만으로 커넥션 풀이 고갈된다. 저장(save)은 Spring
    // Data가 호출마다 개별 트랜잭션으로 처리하므로 그대로 안전하다. 대신 LLM 실패 시 이미 저장된
    // userMessage가 롤백되지 않고 남을 수 있다(응답 없는 사용자 메시지) - 감내 가능한 트레이드오프.
    public ConsultationResponse consult(ConsultationRequest request, Long userId) {
        boolean isNewSession = isBlank(request.sessionId());
        String sessionId = isNewSession ? UUID.randomUUID().toString() : request.sessionId();

        // 게스트가 새 세션을 시작할 때만 guest_code를 새로 발급한다 - 세션을 이어가는 요청이면
        // 클라이언트가 이미 갖고 있는 값을 그대로 쓴다 (재발급하면 이전 메시지와 소유권이 끊어짐).
        boolean guestCodeIssued = userId == null && isBlank(request.guestCode());
        String guestCode = userId != null ? null : (guestCodeIssued ? generateGuestCode() : request.guestCode());

        // 후속 질문("그럼 며칠 지나면 병원 가야해요?")은 이전 대화를 알아야 뭘 묻는지 이해가 된다 -
        // 새 세션이면 이전 대화가 없으니 빈 목록, 아니면 이번 사용자 메시지를 저장하기 전에(=순수하게
        // "이전" 턴만) 최근 것만 잘라서 가져온다.
        List<AiConsultation> priorTurns = isNewSession ? List.of() : recentTurns(sessionId);

        AiConsultation userMessage = aiConsultationRepository.save(AiConsultation.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionRoot(isNewSession)
                .guestCode(guestCode)
                .senderType(SenderType.USER)
                .content(request.query())
                .regenerated(false)
                .build());

        String searchQuery = buildSearchQuery(priorTurns, ragRetrievalService.rewriteForSearch(request.query()));
        List<List<Document>> docGroups = ragRetrievalService.retrieveGrouped(searchQuery);
        List<Document> docs = docGroups.stream().flatMap(List::stream).toList();
        // 검색어에는 안 넣는다 - "복통, 고혈압" 같은 병명이 섞이면 지금 증상과 무관한 문서를 끌어올 수
        // 있다. 프롬프트에만 참고 정보로 얹어서, 관련 있을 때만 모델이 알아서 반영하게 한다.
        String healthProfileSummary = buildHealthProfileSummary(userId);
        GenerationResult result = docs.isEmpty()
                ? GenerationResult.noResult()
                : ensureMultiInterpretationCoverage(
                        generate(request.query(), docs, buildConversationHistory(priorTurns), healthProfileSummary),
                        docGroups, docs);

        AiConsultation aiMessage = aiConsultationRepository.save(AiConsultation.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionRoot(false)
                .guestCode(guestCode)
                .symptomKeyword(result.symptomKeyword())
                .senderType(SenderType.AI)
                .content(result.plainText())
                .regenerated(false)
                .parentId(userMessage.getId())
                .build());

        saveReferenceSources(aiMessage.getId(), result.citedSources());

        List<ConsultationResponse.AnswerSegment> answer = buildAnswer(request.query(), result.segments());

        return new ConsultationResponse(
                answer, result.citedSources(), aiMessage.getId(), sessionId,
                guestCodeIssued ? guestCode : null, ConsultationDisclaimer.TEXT);
    }

    // 응급 징후가 있는데 모델이 119 안내를 빠뜨렸을 때만 강제 문장을 추가한다(이미 안내했으면 중복 방지).
    private List<ConsultationResponse.AnswerSegment> buildAnswer(
            String query, List<ConsultationResponse.AnswerSegment> segments) {
        if (!EmergencySignalGuard.containsEmergencySignal(query)) {
            return segments;
        }

        boolean alreadyAdvised119 = segments.stream().anyMatch(s -> s.text().contains("119"));
        if (alreadyAdvised119) {
            // 응급 키워드는 있었지만 모델이 이미 알아서 119 안내를 넣었다 - 정상 경로, 중복 추가 안 함.
            log.info("응급 징후 감지, 모델이 이미 119 안내를 포함해 코드 추가 없이 통과. query=\"{}\"", query);
            return segments;
        }

        // 마지막 방어선 실제 발동 - 모델이 119 규칙(CONSULT_PROMPT 6번)을 놓쳤다.
        log.warn("응급 징후 감지했으나 모델 응답에 119 안내가 없어 코드가 강제로 추가했습니다. query=\"{}\"", query);
        return Stream.concat(Stream.of(FORCED_EMERGENCY_NOTICE), segments.stream()).toList();
    }

    // 세션의 최근 대화를 시간순으로 가져온다(이번 사용자 메시지 저장 전이라 "이전" 턴만 잡힌다).
    // 길게 이어진 세션이라도 프롬프트/검색어에 태울 건 최근 몇 개로만 자른다(MAX_HISTORY_MESSAGES).
    private List<AiConsultation> recentTurns(String sessionId) {
        List<AiConsultation> all = aiConsultationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return all.size() > MAX_HISTORY_MESSAGES
                ? all.subList(all.size() - MAX_HISTORY_MESSAGES, all.size())
                : all;
    }

    // 후속 질문만 그대로 벡터 검색하면 증상 키워드가 없어서 엉뚱한 문서가 걸린다. 이전 사용자
    // 발화들을 검색어에 같이 넣어서 보정한다. currentQuery는 이미 ragRetrievalService.rewriteForSearch를
    // 거친 검색용 문구다.
    private String buildSearchQuery(List<AiConsultation> priorTurns, String currentQuery) {
        String priorUserText = priorTurns.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .map(AiConsultation::getContent)
                .collect(Collectors.joining(" "));
        return priorUserText.isBlank() ? currentQuery : priorUserText + " " + currentQuery;
    }

    // 모델이 후속 질문에 자연스럽게 이어 답하도록 최근 대화를 프롬프트에 텍스트로 준다.
    private String buildConversationHistory(List<AiConsultation> priorTurns) {
        return priorTurns.stream()
                .map(m -> "%s: %s".formatted(m.getSenderType() == SenderType.USER ? "사용자" : "AI", m.getContent()))
                .collect(Collectors.joining("\n"));
    }

    // 로그인 사용자만 - 게스트는 프로필이 없다. 기저질환/복용약/알레르기 중 채워진 것만 담는다.
    private String buildHealthProfileSummary(Long userId) {
        if (userId == null) {
            return "";
        }
        return healthProfileRepository.findByUserId(userId).map(this::formatHealthProfile).orElse("");
    }

    private String formatHealthProfile(HealthProfile profile) {
        List<String> parts = new ArrayList<>();
        if (hasText(profile.getDiseases())) {
            parts.add("기저질환: " + profile.getDiseases());
        }
        if (hasText(profile.getMedications())) {
            parts.add("복용약: " + profile.getMedications());
        }
        if (hasText(profile.getAllergies())) {
            parts.add("알레르기: " + profile.getAllergies());
        }
        return String.join(", ", parts);
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    // RagRetrievalService.retrieveGrouped가 "|"로 해석을 나눈 경우(예: "목"→목뼈 계통/인후 계통),
    // CONSULT_PROMPT(규칙 10)에 "두 계통 다 언급하라"고 지시해뒀지만 LLM이 실제로 따르는 건
    // 확률적이다(실측 3회 중 1회만 반영). 그래서 모델이 한쪽 계통을 answer에서 아예 안 다뤘으면
    // 코드가 문장을 강제로 덧붙인다. 해석이 하나뿐이거나(docGroups.size() <= 1), 애초에 아무것도
    // 인용 안 된 답변(noResult/생성실패/진단가드 폴백)이면 건드리지 않는다 - 안전 문구에 억지로
    // 질병명을 덧붙이면 오히려 혼란을 준다.
    private GenerationResult ensureMultiInterpretationCoverage(
            GenerationResult result, List<List<Document>> docGroups, List<Document> docs) {
        if (docGroups.size() <= 1 || result.citedSources().isEmpty()) {
            return result;
        }

        Set<String> citedDiseases = result.citedSources().stream()
                .map(ConsultationResponse.Source::disease)
                .collect(Collectors.toSet());

        List<ConsultationResponse.AnswerSegment> extraSegments = new ArrayList<>();
        List<ConsultationResponse.Source> extraSources = new ArrayList<>();
        for (List<Document> group : docGroups) {
            if (group.isEmpty()) {
                continue;
            }
            Document top = group.get(0); // 그룹 안에서 가장 점수 높은 문서 - search()가 이미 점수순으로 준다.
            String disease = (String) top.getMetadata().get("disease");
            if (citedDiseases.contains(disease)) {
                continue; // 모델이 이미 이 계통을 언급함
            }
            int index = docs.indexOf(top) + 1; // buildContext의 [번호]와 맞춘다(1-based)
            extraSegments.add(new ConsultationResponse.AnswerSegment(
                    "%s이(가) 원인일 가능성도 있습니다. 증상이 이어지거나 다른 양상이면 병원에서 감별 진료를 받아보세요."
                            .formatted(disease),
                    List.of(index)));
            extraSources.add(toSource(index, top));
        }

        if (extraSegments.isEmpty()) {
            return result;
        }

        List<ConsultationResponse.AnswerSegment> segments =
                Stream.concat(result.segments().stream(), extraSegments.stream()).toList();
        List<ConsultationResponse.Source> citedSources =
                Stream.concat(result.citedSources().stream(), extraSources.stream()).toList();
        String plainText = segments.stream()
                .map(ConsultationResponse.AnswerSegment::text)
                .collect(Collectors.joining(" "));
        return new GenerationResult(segments, citedSources, plainText, result.symptomKeyword());
    }

    private GenerationResult generate(
            String query, List<Document> docs, String conversationHistory, String healthProfileSummary) {
        StringBuilder userPrompt = new StringBuilder();
        if (!conversationHistory.isBlank()) {
            userPrompt.append("[이전 대화]\n").append(conversationHistory).append("\n\n");
        }
        if (!healthProfileSummary.isBlank()) {
            userPrompt.append("[사용자 건강정보]\n").append(healthProfileSummary).append("\n\n");
        }
        userPrompt.append("[참고자료]\n").append(ragRetrievalService.buildContext(docs))
                .append("\n\n[질문]\n").append(query);

        RawAnswer raw = generateRawAnswer(query, userPrompt.toString());
        if (raw == null) {
            return GenerationResult.generationFailed();
        }

        List<ConsultationResponse.Source> sources = IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> toSource(i, docs.get(i - 1)))
                .toList();
        Set<Integer> validIndexes = sources.stream()
                .map(ConsultationResponse.Source::index)
                .collect(Collectors.toSet());

        // 모델이 규칙을 어기고 존재하지 않는 번호를 인용해도(환각) 조용히 걸러낸다 -
        // 최악의 경우 "근거 없는 문장"이 될 뿐, 잘못된 출처가 붙는 일은 없다.
        // segments 배열 자체는 위에서 확인했지만, 개별 원소는 LLM이 스키마를 어기고 text/sourceIndexes를
        // null로 줄 수 있다 - text가 없으면 보여줄 내용이 없으니 걸러내고, sourceIndexes가 없으면
        // (그대로 .stream() 호출 시 NPE) 인용 없는 문장으로 취급한다.
        List<ConsultationResponse.AnswerSegment> segments = raw.segments().stream()
                .filter(segment -> segment != null && segment.text() != null && !segment.text().isBlank())
                .map(segment -> new ConsultationResponse.AnswerSegment(
                        segment.text(),
                        segment.sourceIndexes() == null
                                ? List.of()
                                : segment.sourceIndexes().stream().filter(validIndexes::contains).toList()))
                .toList();

        // 검색은 됐지만 실제 답변 문장에서 한 번도 인용되지 않은 문서는 "출처"로 보여주지 않는다.
        Set<Integer> citedIndexes = segments.stream()
                .flatMap(segment -> segment.sourceIndexes().stream())
                .collect(Collectors.toSet());
        List<ConsultationResponse.Source> citedSources = sources.stream()
                .filter(source -> citedIndexes.contains(source.index()))
                .toList();

        String plainText = segments.stream()
                .map(ConsultationResponse.AnswerSegment::text)
                .collect(Collectors.joining(" "));

        // 마지막 방어선: 프롬프트로 진단하지 말라고 지시해도(CONSULT_PROMPT) 모델이 어길 수 있다
        // (ConsultationRegenerationService와 같은 이유). 걸리면 부분 수정 대신 통째로 안전한 문구로
        // 교체한다 - 부분 수정은 문장을 어색하게 만들 위험이 크다.
        if (DiagnosisLanguageGuard.containsDiagnosticLanguage(plainText)) {
            log.warn("1차 답변에서 진단형 표현이 감지되어 폴백 문구로 대체합니다. query=\"{}\"", query);
            return GenerationResult.diagnosticLanguageDetected();
        }

        // 체크리스트 생성(ChecklistService)이 이 키워드를 그대로 쓰므로, 검색만 되고 답변에서 실제로
        // 인용되지 않은 문서(=답변과 무관할 수 있는 문서)의 질병명이 아니라 실제로 인용된 문서에서
        // 뽑아야 한다. 인용된 게 하나도 없으면(citedSources 비어있음) 그때만 최상위 검색 결과로 폴백한다.
        String symptomKeyword = citedSources.isEmpty()
                ? (String) docs.get(0).getMetadata().get("disease")
                : citedSources.get(0).disease();

        return new GenerationResult(segments, citedSources, plainText, symptomKeyword);
    }

    // LLM 호출 자체가 실패(타임아웃/429/네트워크 오류)하거나 스키마를 벗어난 JSON(예: segments 누락)을
    // 내놓는 경우가 실제로 있었다 - 둘 다 재시도 대상으로 묶는다(ChecklistService.generateItemsWithRetry
    // 와 같은 구조). 모두 실패하면 null을 돌려주고, 호출부가 GenerationResult.generationFailed()로 처리한다.
    private RawAnswer generateRawAnswer(String query, String userPrompt) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RawAnswer raw;
            try {
                raw = chatClient.prompt()
                        .system(AiConsultationPrompts.CONSULT_PROMPT)
                        .user(userPrompt)
                        .call()
                        .entity(RawAnswer.class);
            } catch (Exception e) {
                log.warn("1차 답변 LLM 호출이 실패했습니다 (시도 {}/{}). query=\"{}\"", attempt, MAX_ATTEMPTS, query, e);
                continue;
            }
            if (raw != null && raw.segments() != null) {
                return raw;
            }
            log.warn("1차 답변 LLM 구조화 출력이 예상 스키마를 벗어났습니다 (시도 {}/{}). query=\"{}\", raw={}",
                    attempt, MAX_ATTEMPTS, query, raw);
        }
        return null;
    }

    private void saveReferenceSources(Long consultationId, List<ConsultationResponse.Source> sources) {
        for (ConsultationResponse.Source source : sources) {
            referenceSourceRepository.save(ReferenceSource.builder()
                    .consultationId(consultationId)
                    .sourceName(source.sourceName())
                    .relevanceNote("%s - %s".formatted(source.disease(), source.section()))
                    .build());
        }
    }

    private ConsultationResponse.Source toSource(int index, Document doc) {
        return new ConsultationResponse.Source(
                index,
                (String) doc.getMetadata().get("disease"),
                (String) doc.getMetadata().get("section"),
                (String) doc.getMetadata().get("source"),
                (String) doc.getMetadata().get("cntntsSn"));
    }

    private String generateGuestCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // consult() 내부의 세 갈래(정상 생성/검색 결과 없음/LLM 출력 이상)를 하나의 형태로 모으기 위한 값 객체.
    private record GenerationResult(
            List<ConsultationResponse.AnswerSegment> segments,
            List<ConsultationResponse.Source> citedSources,
            String plainText,
            String symptomKeyword) {

        static GenerationResult noResult() {
            String text = "제공된 정보로는 답변드리기 어렵습니다. 증상이 심각하다면 즉시 119에 신고해주세요.";
            return new GenerationResult(
                    List.of(new ConsultationResponse.AnswerSegment(text, List.of())), List.of(), text, null);
        }

        static GenerationResult generationFailed() {
            String text = "일시적인 오류로 답변을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.";
            return new GenerationResult(
                    List.of(new ConsultationResponse.AnswerSegment(text, List.of())), List.of(), text, null);
        }

        static GenerationResult diagnosticLanguageDetected() {
            String text = "죄송합니다, 답변을 안전하게 정리하는 중 문제가 발생했습니다. 제공된 참고자료로는 확정적인 "
                    + "안내가 어려우니, 증상이 지속되거나 심해지면 병원 진료를 받아보세요.";
            return new GenerationResult(
                    List.of(new ConsultationResponse.AnswerSegment(text, List.of())), List.of(), text, null);
        }
    }
}
