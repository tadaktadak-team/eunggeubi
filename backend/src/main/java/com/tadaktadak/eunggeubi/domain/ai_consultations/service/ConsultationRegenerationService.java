package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawRegeneratedAnswer;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistResponseRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ReferenceSourceRepository;
import com.tadaktadak.eunggeubi.domain.first_aid.service.FirstAidGuideService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

// 체크리스트 응답을 반영해서 이전 AI 답변을 "재생성"한다 - 기존 응답을 덮어쓰지 않고 새 ai_consultations
// 행으로 append한다(is_regenerated=true, based_on_response_id=원본 응답 id).
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationRegenerationService {

    private static final String NO_MATCH_MESSAGE = "제공된 정보로는 더 구체적인 안내가 어렵습니다. 증상이 지속되거나 심해지면 병원 진료를 받아보세요.";
    private static final String DIAGNOSTIC_LANGUAGE_FALLBACK =
            "죄송합니다, 안내 문구를 다시 정리하는 중 문제가 발생했습니다. 제공된 참고자료로는 확정적인 안내가 어려우니, "
                    + "증상이 지속되거나 심해지면 병원 진료를 받아보세요.";

    // AiConsultationService.MAX_ATTEMPTS와 같은 이유 - LLM 호출 자체 실패(타임아웃/429/네트워크 오류)도
    // 재시도 대상이다. 안 잡으면 일시적인 오류 한 번에 전체 요청이 500으로 끝난다.
    private static final int MAX_ATTEMPTS = 3;

    private final AiConsultationRepository aiConsultationRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
    private final ReferenceSourceRepository referenceSourceRepository;
    private final RagRetrievalService ragRetrievalService;
    private final ChatClient chatClient;
    private final FirstAidGuideService firstAidGuideService;

    // ponytail: 트랜잭션으로 안 감싼다 - RAG 검색+LLM 호출 동안 DB 커넥션을 잡고 있으면 동시 요청
    // 몇 개만으로 커넥션 풀이 고갈된다(저장은 Spring Data가 save() 호출마다 개별 트랜잭션으로 처리).
    public RegenerateResponse regenerate(Long consultationId, Long userId, String guestCode) {
        AiConsultation baseMessage = getOwnedAiMessage(consultationId, userId, guestCode);
        String symptomText = resolveSymptomText(baseMessage);
        List<String> checkedItems = resolveCheckedItems(baseMessage);

        String combinedQuery = checkedItems.isEmpty()
                ? symptomText
                : "%s (%s 있음)".formatted(symptomText, String.join(", ", checkedItems));
        RegenerateResponse.RelatedAidGuide relatedAidGuide = findRelatedAidGuide(combinedQuery);

        List<Document> docs = ragRetrievalService.retrieve(combinedQuery);
        if (docs.isEmpty()) {
            return persistAndBuild(baseMessage, NO_MATCH_MESSAGE, List.of(), relatedAidGuide);
        }

        String userPrompt = "[참고자료]\n%s\n\n[증상]\n%s\n\n[체크리스트에서 해당한다고 응답한 항목]\n%s".formatted(
                ragRetrievalService.buildContext(docs), symptomText,
                checkedItems.isEmpty() ? "없음" : String.join("\n", checkedItems));
        RawRegeneratedAnswer raw = generateRawAnswer(consultationId, userPrompt);
        if (raw == null) {
            return persistAndBuild(baseMessage, DIAGNOSTIC_LANGUAGE_FALLBACK, List.of(), relatedAidGuide);
        }

        // 마지막 방어선: 프롬프트로 진단하지 말라고 지시해도 모델이 어길 수 있다. 걸리면 부분 수정 대신
        // 통째로 안전한 문구로 교체한다 (부분 수정은 문장을 어색하게 만들 위험이 크다).
        if (DiagnosisLanguageGuard.containsDiagnosticLanguage(raw.message())) {
            log.warn("재생성 응답에서 진단형 표현이 감지되어 폴백 문구로 대체합니다. consultationId={}", consultationId);
            return persistAndBuild(baseMessage, DIAGNOSTIC_LANGUAGE_FALLBACK, List.of(), relatedAidGuide);
        }

        List<ConsultationSource> sources = IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> toSource(i, docs.get(i - 1)))
                .toList();
        Set<Integer> validIndexes = sources.stream().map(ConsultationSource::index).collect(Collectors.toSet());
        List<Integer> citedIndexes = raw.citedSourceIndexes() == null
                ? List.of()
                : raw.citedSourceIndexes().stream().filter(validIndexes::contains).distinct().toList();
        List<ConsultationSource> citedSources = sources.stream()
                .filter(source -> citedIndexes.contains(source.index()))
                .toList();

        return persistAndBuild(baseMessage, raw.message(), citedSources, relatedAidGuide);
    }

    // LLM 호출 자체가 실패하거나 스키마를 벗어난 응답(message 누락/빈 문자열)을 내놓는 경우를 모두
    // 재시도 대상으로 묶는다(ChecklistService.generateItemsWithRetry와 같은 구조). 모두 실패하면 null을
    // 돌려주고, 호출부가 DIAGNOSTIC_LANGUAGE_FALLBACK으로 처리한다.
    private RawRegeneratedAnswer generateRawAnswer(Long consultationId, String userPrompt) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RawRegeneratedAnswer raw;
            try {
                raw = chatClient.prompt()
                        .system(AiConsultationPrompts.REGENERATE_PROMPT)
                        .user(userPrompt)
                        .call()
                        .entity(RawRegeneratedAnswer.class);
            } catch (Exception e) {
                log.warn("재생성 LLM 호출이 실패했습니다 (시도 {}/{}). consultationId={}", attempt, MAX_ATTEMPTS, consultationId, e);
                continue;
            }
            if (raw != null && raw.message() != null && !raw.message().isBlank()) {
                return raw;
            }
            log.warn("재생성 LLM 구조화 출력이 예상 스키마를 벗어났습니다 (시도 {}/{}). consultationId={}, raw={}",
                    attempt, MAX_ATTEMPTS, consultationId, raw);
        }
        return null;
    }

    // 화상/코피/골절/기도막힘 4개로 한정하지 않고 health_info 컬렉션 전체를 대상으로 찾는다 -
    // FirstAidGuideService.search()가 실제 현장 응급처치 콘텐츠가 있는 주제만 걸러서 돌려주므로
    // (없으면 Optional.empty()) 이 체크만으로 "관련 응급처치가 실제로 존재하는지"까지 확인된다.
    // ponytail: search()가 여기서 한 번, 사용자가 배너를 눌러 FirstAidGuide 화면에 들어갈 때 한 번 더
    // 호출돼서 같은 내용을 두 번 생성한다(캐싱 없음) - 재생성은 체크리스트 제출당 1회뿐이라 감내 가능한
    // 비용이지만, 호출 빈도가 늘면 캐싱을 고려해야 한다.
    private RegenerateResponse.RelatedAidGuide findRelatedAidGuide(String combinedQuery) {
        return firstAidGuideService.search(combinedQuery)
                .map(guide -> new RegenerateResponse.RelatedAidGuide(guide.situation(), guide.title()))
                .orElse(null);
    }

    private RegenerateResponse persistAndBuild(AiConsultation baseMessage, String message,
                                                List<ConsultationSource> citedSources,
                                                RegenerateResponse.RelatedAidGuide relatedAidGuide) {
        AiConsultation regenerated = aiConsultationRepository.save(AiConsultation.builder()
                .userId(baseMessage.getUserId())
                .sessionId(baseMessage.getSessionId())
                .sessionRoot(false)
                .guestCode(baseMessage.getGuestCode())
                .symptomKeyword(baseMessage.getSymptomKeyword())
                .senderType(SenderType.AI)
                .content(message)
                .regenerated(true)
                .parentId(baseMessage.getParentId())
                .basedOnResponseId(baseMessage.getId())
                .build());

        List<RegenerateResponse.RegeneratedSource> sourceDtos = citedSources.stream()
                .map(source -> {
                    ReferenceSource saved = referenceSourceRepository.save(ReferenceSource.builder()
                            .consultationId(regenerated.getId())
                            .sourceName(source.sourceName())
                            .relevanceNote("%s - %s".formatted(source.disease(), source.section()))
                            .build());
                    return new RegenerateResponse.RegeneratedSource(
                            saved.getId(), "%s - %s".formatted(source.disease(), source.section()), source.sourceName());
                })
                .toList();

        return new RegenerateResponse(message, false, sourceDtos, ConsultationDisclaimer.TEXT, relatedAidGuide);
    }

    private AiConsultation getOwnedAiMessage(Long consultationId, Long userId, String guestCode) {
        AiConsultation consultation = aiConsultationRepository.findById(consultationId)
                .orElseThrow(() -> new IllegalArgumentException("상담 내역을 찾을 수 없습니다."));
        if (!consultation.isOwnedBy(userId, guestCode)) {
            throw new IllegalArgumentException("본인 상담 세션이 아닙니다.");
        }
        if (consultation.getSenderType() != SenderType.AI) {
            throw new IllegalArgumentException("AI 응답에 대해서만 재생성할 수 있습니다.");
        }
        return consultation;
    }

    private String resolveSymptomText(AiConsultation aiMessage) {
        if (aiMessage.getParentId() == null) {
            return aiMessage.getContent();
        }
        return aiConsultationRepository.findById(aiMessage.getParentId())
                .map(AiConsultation::getContent)
                .orElse(aiMessage.getContent());
    }

    // 체크리스트에서 사용자가 "해당한다"고 체크한 항목들 (checklist_responses.selected_items).
    // 체크리스트는 항상 "최초" AI 응답의 consultationId에 달려있다 - 재생성된 응답을 또 재생성하면
    // basedOnResponseId를 계속 따라 올라가서 그 최초 응답을 찾아야, 두 번째 재생성에서도 체크리스트
    // 결과가 반영된다(안 그러면 체크리스트를 못 찾아 매번 checkedItems가 빈 값으로 헛돈다).
    private List<String> resolveCheckedItems(AiConsultation message) {
        Long checklistOwnerId = resolveChecklistOwner(message).getId();
        return checklistRepository.findTopByConsultationIdOrderByCreatedAtDesc(checklistOwnerId)
                .flatMap(checklist -> checklistResponseRepository
                        .findTopByChecklistIdOrderByCreatedAtDesc(checklist.getId())
                        .map(ChecklistResponse::selectedItemList))
                .orElse(List.of());
    }

    private AiConsultation resolveChecklistOwner(AiConsultation message) {
        AiConsultation current = message;
        while (current.getBasedOnResponseId() != null) {
            current = aiConsultationRepository.findById(current.getBasedOnResponseId())
                    .orElseThrow(() -> new IllegalArgumentException("원본 상담을 찾을 수 없습니다."));
        }
        return current;
    }

    private ConsultationSource toSource(int index, Document doc) {
        return new ConsultationSource(
                index,
                (String) doc.getMetadata().get("disease"),
                (String) doc.getMetadata().get("section"),
                (String) doc.getMetadata().get("source"));
    }

    // AiConsultationService.ConsultationResponse.Source와 형태는 같지만 이 서비스 안에서만 쓰는
    // 중간 표현이라 별도 타입으로 둔다 (최종 출력 포맷은 RegenerateResponse.RegeneratedSource).
    private record ConsultationSource(int index, String disease, String section, String sourceName) {
    }
}
