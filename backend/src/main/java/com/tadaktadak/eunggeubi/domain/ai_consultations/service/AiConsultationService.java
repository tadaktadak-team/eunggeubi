package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawAnswer;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ReferenceSourceRepository;
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
import org.springframework.transaction.annotation.Transactional;

// health_info 컬렉션에서 관련 문서를 검색한 뒤, 그 문서만 근거로 LLM 답변을 생성한다(RAG).
// 답변은 문장(segment) 단위로 쪼개고 각 문장이 인용한 참고자료 번호를 함께 받아서,
// 프론트에서 문장별로 출처를 붙일 수 있게 한다.
// 대화 한 턴(사용자 메시지 + AI 응답)을 ai_consultations/reference_sources에 저장한다 - 이후 체크리스트
// 생성/재생성(ChecklistService, ConsultationRegenerationService)이 이 저장된 상담을 이어서 쓴다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConsultationService {

    // "의료 자문을 대체하지 않습니다" 고지는 모델에게 매번 붙이라고 프롬프트로 부탁하는 대신
    // 서버가 응답 끝에 결정적으로 붙인다 - 컴플라이언스성 문구를 모델의 지시 준수에만 맡기면
    // 실제로 빠뜨리는 경우가 있었다. DB에는 중복 저장하지 않고 응답에서만 붙인다.
    private static final ConsultationResponse.AnswerSegment DISCLAIMER =
            new ConsultationResponse.AnswerSegment("이 안내는 의료 자문을 대체하지 않습니다.", List.of());

    private final RagRetrievalService ragRetrievalService;
    private final ChatClient chatClient;
    private final AiConsultationRepository aiConsultationRepository;
    private final ReferenceSourceRepository referenceSourceRepository;

    @Transactional
    public ConsultationResponse consult(ConsultationRequest request, Long userId) {
        boolean isNewSession = isBlank(request.sessionId());
        String sessionId = isNewSession ? UUID.randomUUID().toString() : request.sessionId();

        // 게스트가 새 세션을 시작할 때만 guest_code를 새로 발급한다 - 세션을 이어가는 요청이면
        // 클라이언트가 이미 갖고 있는 값을 그대로 쓴다 (재발급하면 이전 메시지와 소유권이 끊어짐).
        boolean guestCodeIssued = userId == null && isBlank(request.guestCode());
        String guestCode = userId != null ? null : (guestCodeIssued ? generateGuestCode() : request.guestCode());

        AiConsultation userMessage = aiConsultationRepository.save(AiConsultation.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionRoot(isNewSession)
                .guestCode(guestCode)
                .senderType(SenderType.USER)
                .content(request.query())
                .regenerated(false)
                .build());

        List<Document> docs = ragRetrievalService.retrieve(request.query());
        GenerationResult result = docs.isEmpty() ? GenerationResult.noResult() : generate(request.query(), docs);

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

        List<ConsultationResponse.AnswerSegment> answer =
                Stream.concat(result.segments().stream(), Stream.of(DISCLAIMER)).toList();

        return new ConsultationResponse(
                answer, result.citedSources(), aiMessage.getId(), sessionId,
                guestCodeIssued ? guestCode : null);
    }

    private GenerationResult generate(String query, List<Document> docs) {
        RawAnswer raw = chatClient.prompt()
                .system(AiConsultationPrompts.CONSULT_PROMPT)
                .user("[참고자료]\n%s\n\n[질문]\n%s".formatted(ragRetrievalService.buildContext(docs), query))
                .call()
                .entity(RawAnswer.class);

        // 모델이 스키마를 벗어난 JSON(예: segments 누락)을 내놓는 경우가 실제로 있었다 -
        // LLM 출력은 신뢰할 수 없는 입력이므로 여기서 막지 않으면 NPE로 그대로 500이 난다.
        if (raw == null || raw.segments() == null) {
            log.warn("LLM 구조화 출력이 예상 스키마를 벗어났습니다. query=\"{}\", raw={}", query, raw);
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
        List<ConsultationResponse.AnswerSegment> segments = raw.segments().stream()
                .map(segment -> new ConsultationResponse.AnswerSegment(
                        segment.text(),
                        segment.sourceIndexes().stream().filter(validIndexes::contains).toList()))
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
        String symptomKeyword = (String) docs.get(0).getMetadata().get("disease");

        return new GenerationResult(segments, citedSources, plainText, symptomKeyword);
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
    }
}
