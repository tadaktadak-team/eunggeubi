package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawAnswer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

// health_info 컬렉션에서 관련 문서를 검색한 뒤, 그 문서만 근거로 LLM 답변을 생성한다(RAG).
// 답변은 문장(segment) 단위로 쪼개고 각 문장이 인용한 참고자료 번호를 함께 받아서,
// 프론트에서 문장별로 출처를 붙일 수 있게 한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConsultationService {

    // 질문과 관련 없는 문서가 답변에 섞이는 걸 막기 위한 최소 유사도. 이 밑으로는 검색 결과에서 제외한다.
    private static final double SIMILARITY_THRESHOLD = 0.5;
    private static final int TOP_K = 5;

    private static final String SYSTEM_PROMPT = """
            너는 질병관리청 국가건강정보포털 자료를 근거로 답변하는 응급/건강정보 안내 도우미다.

            [반드시 지킬 규칙]
            1. 아래 [참고자료]에 있는 내용만 근거로 답한다. 참고자료에 없는 내용은 답하지 않는다.
            2. 참고자료로 답할 수 없는 질문이면 segments를 "제공된 정보로는 답변드리기 어렵습니다."
               한 문장(sourceIndexes: [])만 담아 응답한다. 추측해서 답하지 않는다.
            3. 답변은 짧고 명확한 문장 단위로 나눈다. 한 문장에는 하나의 사실만 담는다.
            4. 각 문장(segment)마다, 그 문장의 근거가 된 [참고자료] 번호를 sourceIndexes에 넣는다.
               - 번호는 반드시 실제로 제공된 [1]~[N] 범위 안에서만 쓴다. 지어내지 않는다.
               - 인사말, 119 안내, "의료 자문이 아닙니다" 같은 고지성 문장은 sourceIndexes를 빈 배열로 둔다.
               - 여러 참고자료를 종합한 문장이면 해당 번호를 모두 넣는다 (예: [1, 3]).
            5. 의식 소실, 심한 출혈, 호흡곤란 등 응급 징후가 질문에 있으면 반드시 첫 문장에서
               "즉시 119에 신고하세요"를 안내한다 (sourceIndexes: []).
            6. 존댓말을 쓴다.
            """;

    // "의료 자문을 대체하지 않습니다" 고지는 모델에게 매번 붙이라고 프롬프트로 부탁하는 대신
    // 서버가 응답 끝에 결정적으로 붙인다 - 컴플라이언스성 문구를 모델의 지시 준수에만 맡기면
    // 실제로 빠뜨리는 경우가 있었다.
    private static final ConsultationResponse.AnswerSegment DISCLAIMER =
            new ConsultationResponse.AnswerSegment("이 안내는 의료 자문을 대체하지 않습니다.", List.of());

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public ConsultationResponse consult(String query) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build());

        if (docs.isEmpty()) {
            return noResultResponse();
        }

        RawAnswer raw = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("[참고자료]\n%s\n\n[질문]\n%s".formatted(buildContext(docs), query))
                .call()
                .entity(RawAnswer.class);

        // 모델이 스키마를 벗어난 JSON(예: segments 누락)을 내놓는 경우가 실제로 있었다 -
        // LLM 출력은 신뢰할 수 없는 입력이므로 여기서 막지 않으면 NPE로 그대로 500이 난다.
        if (raw == null || raw.segments() == null) {
            log.warn("LLM 구조화 출력이 예상 스키마를 벗어났습니다. query=\"{}\", raw={}", query, raw);
            return generationFailedResponse();
        }

        List<ConsultationResponse.Source> sources = IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> toSource(i, docs.get(i - 1)))
                .toList();
        Set<Integer> validIndexes = sources.stream()
                .map(ConsultationResponse.Source::index)
                .collect(Collectors.toSet());

        // 모델이 규칙 4를 어기고 존재하지 않는 번호를 인용해도(환각) 조용히 걸러낸다 -
        // 최악의 경우 "근거 없는 문장"이 될 뿐, 잘못된 출처가 붙는 일은 없다.
        List<ConsultationResponse.AnswerSegment> segments = raw.segments().stream()
                .map(segment -> new ConsultationResponse.AnswerSegment(
                        segment.text(),
                        segment.sourceIndexes().stream().filter(validIndexes::contains).toList()))
                .toList();

        // 검색은 됐지만 실제 답변 문장에서 한 번도 인용되지 않은 문서는 "출처"로 보여주지 않는다.
        // (citedIndexes는 DISCLAIMER를 넣기 전에 계산 - 어차피 근거 없는 문장이라 결과에 안 섞인다)
        Set<Integer> citedIndexes = segments.stream()
                .flatMap(segment -> segment.sourceIndexes().stream())
                .collect(Collectors.toSet());
        List<ConsultationResponse.Source> citedSources = sources.stream()
                .filter(source -> citedIndexes.contains(source.index()))
                .toList();

        List<ConsultationResponse.AnswerSegment> answer =
                Stream.concat(segments.stream(), Stream.of(DISCLAIMER)).toList();

        return new ConsultationResponse(answer, citedSources);
    }

    private String buildContext(List<Document> docs) {
        return IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> "[%d] (%s) %s".formatted(
                        i, docs.get(i - 1).getMetadata().get("disease"), docs.get(i - 1).getText()))
                .collect(Collectors.joining("\n\n"));
    }

    private ConsultationResponse.Source toSource(int index, Document doc) {
        return new ConsultationResponse.Source(
                index,
                (String) doc.getMetadata().get("disease"),
                (String) doc.getMetadata().get("section"),
                (String) doc.getMetadata().get("source"),
                (String) doc.getMetadata().get("cntntsSn"));
    }

    private ConsultationResponse noResultResponse() {
        ConsultationResponse.AnswerSegment segment = new ConsultationResponse.AnswerSegment(
                "제공된 정보로는 답변드리기 어렵습니다. 증상이 심각하다면 즉시 119에 신고해주세요.", List.of());
        return new ConsultationResponse(List.of(segment, DISCLAIMER), List.of());
    }

    private ConsultationResponse generationFailedResponse() {
        ConsultationResponse.AnswerSegment segment = new ConsultationResponse.AnswerSegment(
                "일시적인 오류로 답변을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.", List.of());
        return new ConsultationResponse(List.of(segment, DISCLAIMER), List.of());
    }
}
