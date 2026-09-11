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

    private final AiConsultationRepository aiConsultationRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistResponseRepository checklistResponseRepository;
    private final ReferenceSourceRepository referenceSourceRepository;
    private final RagRetrievalService ragRetrievalService;
    private final ChatClient chatClient;

    // ponytail: 트랜잭션으로 안 감싼다 - RAG 검색+LLM 호출 동안 DB 커넥션을 잡고 있으면 동시 요청
    // 몇 개만으로 커넥션 풀이 고갈된다(저장은 Spring Data가 save() 호출마다 개별 트랜잭션으로 처리).
    public RegenerateResponse regenerate(Long consultationId, Long userId, String guestCode) {
        AiConsultation baseMessage = getOwnedAiMessage(consultationId, userId, guestCode);
        String symptomText = resolveSymptomText(baseMessage);
        List<String> checkedItems = resolveCheckedItems(baseMessage);

        String combinedQuery = checkedItems.isEmpty()
                ? symptomText
                : "%s (%s 있음)".formatted(symptomText, String.join(", ", checkedItems));

        List<Document> docs = ragRetrievalService.retrieve(combinedQuery);
        if (docs.isEmpty()) {
            return persistAndBuild(baseMessage, NO_MATCH_MESSAGE, List.of());
        }

        RawRegeneratedAnswer raw = chatClient.prompt()
                .system(AiConsultationPrompts.REGENERATE_PROMPT)
                .user("[참고자료]\n%s\n\n[증상]\n%s\n\n[체크리스트에서 해당한다고 응답한 항목]\n%s".formatted(
                        ragRetrievalService.buildContext(docs), symptomText,
                        checkedItems.isEmpty() ? "없음" : String.join("\n", checkedItems)))
                .call()
                .entity(RawRegeneratedAnswer.class);

        if (raw == null || raw.message() == null || raw.message().isBlank()) {
            log.warn("재생성 LLM 구조화 출력이 예상 스키마를 벗어났습니다. consultationId={}, raw={}", consultationId, raw);
            return persistAndBuild(baseMessage, DIAGNOSTIC_LANGUAGE_FALLBACK, List.of());
        }

        // 마지막 방어선: 프롬프트로 진단하지 말라고 지시해도 모델이 어길 수 있다. 걸리면 부분 수정 대신
        // 통째로 안전한 문구로 교체한다 (부분 수정은 문장을 어색하게 만들 위험이 크다).
        if (DiagnosisLanguageGuard.containsDiagnosticLanguage(raw.message())) {
            log.warn("재생성 응답에서 진단형 표현이 감지되어 폴백 문구로 대체합니다. consultationId={}", consultationId);
            return persistAndBuild(baseMessage, DIAGNOSTIC_LANGUAGE_FALLBACK, List.of());
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

        return persistAndBuild(baseMessage, raw.message(), citedSources);
    }

    private RegenerateResponse persistAndBuild(AiConsultation baseMessage, String message,
                                                List<ConsultationSource> citedSources) {
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

        return new RegenerateResponse(message, false, sourceDtos, ConsultationDisclaimer.TEXT);
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
