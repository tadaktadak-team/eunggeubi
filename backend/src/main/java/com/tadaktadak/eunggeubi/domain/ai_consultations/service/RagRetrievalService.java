package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RawSearchQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

// health_info 컬렉션 검색 로직. AiConsultationService(1차 답변)와 ConsultationRegenerationService(재생성),
// FirstAidGuideService(응급처치 가이드)가 똑같은 검색+컨텍스트 조립 방식을 써야 해서 공용으로 뽑아냈다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {

    // 질문과 관련 없는 문서가 답변에 섞이는 걸 막기 위한 최소 유사도. 이 밑으로는 검색 결과에서 제외한다.
    private static final double SIMILARITY_THRESHOLD = 0.35;
    // 5에서 8로 늘렸다 - 검색어 재작성(AiConsultationService.rewriteQueryForSearch)을 거쳐도 실제로
    // 도움 되는 문서가 top 5 밖에 걸리는 경우가 있었다. 후보를 늘려도 안전한 이유: CONSULT_PROMPT가
    // "참고자료에 없는 내용은 답하지 않는다"를 강제해서, 무관한 후보가 몇 개 섞여도 모델이 그냥
    // 무시할 뿐 답변에 실제로 쓰이진 않는다(sourceIndexes로 인용된 것만 "출처"로 노출됨).
    private static final int TOP_K = 8;

    // "목"처럼 한 단어가 서로 다른 부위/증상을 가리킬 수 있는 경우, 재작성 프롬프트(규칙 5)가
    // "목 통증 경부통 거북목증후군|인후통 편도염 인두염"처럼 "|"로 나눠서 준다. 한 검색어에 다 섞으면
    // 콘텐츠가 많은 쪽(예: 거북목증후군)이 임베딩을 지배해서 다른 쪽(인후통) 문서가 top-K 밖으로
    // 완전히 밀려난다 - topK를 늘려도 안 잡힌다(실측 확인). 그래서 나눠서 각각 검색하고 합친다.
    private static final String INTERPRETATION_DELIMITER = "\\|";

    // search()가 벡터 검색만으로는 놓치는 후보까지 넓게 가져와서 키워드 점수 + RRF로 재순위하기
    // 위한 후보 풀 크기와 threshold. "골절"처럼 짧은 단어는 코사인 유사도가 SIMILARITY_THRESHOLD
    // (0.35) 밑으로 떨어져 후보에서 아예 빠지는 문제(실측 확인)를 흡수하려고 threshold를 낮추고
    // 후보 수를 넉넉히 늘렸다 - 최종 반환은 여전히 topK개, 늘어난 건 재순위할 후보군뿐이다.
    // 24였다가 100으로 늘렸다 - 483개 질병명 전수 벤치마크(실측)에서 정답 문서의 벡터 순위가
    // 24위 밖에 있어 후보에도 못 들던 케이스들이 있었고, 100까지 늘리면 회귀 없이 더 잡혔다.
    private static final int CANDIDATE_POOL_SIZE = 100;
    private static final double LOW_SIMILARITY_THRESHOLD = 0.15;
    // Reciprocal Rank Fusion 상수. Cormack et al.(2009), Elasticsearch/OpenSearch RRF 기본값과 동일.
    private static final int RRF_K = 60;
    // keywordScore의 질병명 완전일치 가산점(아래 keywordScore 메서드 참고)과 같은 값 - 이 값 이상이면
    // "질병명이 쿼리와 완전히 같다"는 뜻이라 byExactMatchThenRrf 정렬에서 최우선으로 끌어올린다.
    private static final double EXACT_MATCH_KEYWORD_SCORE = 3.0;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    // 사용자의 일상어 표현을 검색용 의학 용어로 바꾼다(예: "발이 부어요" -> "부종 다리 발목 붓기") -
    // 실제로 이렇게 안 바꾸면 관련 문서가 top-K 밖으로 밀려서 검색이 아예 안 되는 경우가 있었다.
    // 짧은 단어 하나(예: "골절")도 마찬가지로 그대로 검색하면 유사도가 threshold 밑으로 떨어지는
    // 경우가 실측으로 확인됐다 - 검색에만 쓰고, 원래 표현은 호출부가 별도로 보관해서 쓴다.
    // LLM 호출이라 실패할 수 있어서, 실패하면 원래 질문 그대로 검색하도록 폴백한다.
    public String rewriteForSearch(String query) {
        try {
            RawSearchQuery raw = chatClient.prompt()
                    .system(AiConsultationPrompts.QUERY_REWRITE_PROMPT)
                    .user(query)
                    .call()
                    .entity(RawSearchQuery.class);
            if (raw != null && raw.searchTerms() != null && !raw.searchTerms().isBlank()) {
                return raw.searchTerms();
            }
        } catch (Exception e) {
            log.warn("검색어 재작성 호출이 실패했습니다. 원래 질문으로 검색합니다. query=\"{}\"", query, e);
        }
        return query;
    }

    // 이미 정확한 질병명을 알고 있을 때(FirstAidGuideService) 쓴다 - 검색어 유사도로 후보를 좁히는
    // 대신 disease 메타데이터로 그 질병의 청크만 정확히 가져온다. "골절"처럼 검색어 자체가 다른
    // 질병의 "치료" 섹션과 임베딩상 더 가깝게 나와서 전혀 무관한 결과가 섞이는 문제(실측 확인)를
    // 원천적으로 막는다 - 후보군이 이미 그 질병으로 좁혀졌으니 유사도 threshold도 낮춘다.
    public List<Document> retrieveByExactDisease(String disease) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(disease)
                        .topK(30)
                        .similarityThreshold(0.0)
                        .filterExpression("disease == '%s'".formatted(disease.replace("'", "\\'")))
                        .build());
    }

    public List<Document> retrieve(String query) {
        return retrieveGrouped(query).stream().flatMap(List::stream).toList();
    }

    // retrieve()와 같은 검색이지만, "|"로 나뉜 해석별로 결과를 묶어서 돌려준다. 해석이 하나면
    // (즉 "|"가 없으면) 그룹도 하나뿐이다. AiConsultationService가 이 그룹 단위로 "모델이 어느 한쪽
    // 해석을 답변에서 아예 빼먹었는지"를 확인해서, CONSULT_PROMPT 지시만으로는 안 되는 경우
    // (실측: 확률적으로만 따름) 코드가 강제로 문장을 추가하는 데 쓴다.
    public List<List<Document>> retrieveGrouped(String query) {
        String[] interpretations = query.split(INTERPRETATION_DELIMITER);
        if (interpretations.length <= 1) {
            return List.of(search(query, TOP_K));
        }

        // 해석마다 점수 스케일 자체가 다를 수 있어(예: "거북목" 쪽 문서량이 많아 자체 검색에서도
        // 절대 점수가 더 높게 나옴), 합친 뒤 점수로 한 번 더 자르면 점수가 낮은 해석이 통째로
        // 밀려날 수 있다 - 그래서 해석마다 TOP_K를 공평하게 나눠 갖고, 그 안에서만 점수순으로 뽑는다.
        int perInterpretation = Math.max(1, TOP_K / interpretations.length);
        Set<String> seenIds = new HashSet<>();
        List<List<Document>> groups = new ArrayList<>();
        for (String interpretation : interpretations) {
            List<Document> group = new ArrayList<>();
            for (Document doc : search(interpretation.trim(), perInterpretation)) {
                if (seenIds.add(doc.getId())) {
                    group.add(doc);
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private List<Document> search(String query, int topK) {
        List<Document> candidates = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(CANDIDATE_POOL_SIZE)
                        .similarityThreshold(LOW_SIMILARITY_THRESHOLD)
                        .build());
        return rerankHybrid(query, candidates, topK);
    }

    // 벡터 유사도 순위와 키워드 매치 순위를 RRF로 합쳐 재정렬한다 - 순수 벡터 순위만으로는 top-K
    // 밖으로 밀리던 문서(실측 확인)를 키워드 신호로 다시 끌어올리기 위함. 최종 게이트는 "원래
    // threshold를 넘거나, 키워드 매치가 있는" 문서만 통과시켜서 무관한 저유사도 노이즈는 걸러낸다.
    private List<Document> rerankHybrid(String query, List<Document> candidates, int topK) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        List<String> queryTokens = tokenize(query);
        Map<String, Double> keywordScoreById = candidates.stream()
                .collect(Collectors.toMap(Document::getId, d -> keywordScore(d, query, queryTokens)));

        List<Document> byKeyword = candidates.stream()
                .sorted(Comparator.comparingDouble((Document d) -> keywordScoreById.get(d.getId())).reversed())
                .toList();

        Map<String, Double> rrfScoreById = new HashMap<>();
        for (int rank = 0; rank < candidates.size(); rank++) {
            rrfScoreById.merge(candidates.get(rank).getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
        for (int rank = 0; rank < byKeyword.size(); rank++) {
            rrfScoreById.merge(byKeyword.get(rank).getId(), 1.0 / (RRF_K + rank + 1), Double::sum);
        }

        // 질병명이 쿼리와 완전히 일치하면 RRF 순위와 무관하게 최우선으로 끌어올린다 - RRF는 벡터
        // 순위와 키워드 순위를 50:50으로 섞다 보니, 정답 문서의 벡터 순위가 아주 깊으면(실측: 38위)
        // 키워드가 만점이어도 "키워드 매치는 없지만 벡터 순위가 훨씬 좋은" 문서를 못 이기는 경우가
        // 실측으로 확인됐다(예: "만성비염"). 완전일치는 흔치 않고 신뢰도가 높은 신호라, RRF로 섞지
        // 않고 그 자체로 우선순위를 준다. 완전일치 문서가 여럿이면(같은 질병의 여러 섹션) 그 안에서는
        // 여전히 RRF/키워드 점수로 순서를 매긴다.
        Comparator<Document> byExactMatchThenRrf = Comparator
                .<Document>comparingInt(d -> keywordScoreById.get(d.getId()) >= EXACT_MATCH_KEYWORD_SCORE ? 1 : 0)
                .thenComparingDouble(d -> rrfScoreById.get(d.getId()))
                .thenComparingDouble(d -> keywordScoreById.get(d.getId()))
                .reversed();

        return candidates.stream()
                .filter(d -> (d.getScore() != null && d.getScore() >= SIMILARITY_THRESHOLD)
                        || keywordScoreById.get(d.getId()) > 0.0)
                .sorted(byExactMatchThenRrf)
                .limit(topK)
                .toList();
    }

    // 형태소분석기 없이 disease 메타데이터/본문/섹션명과의 substring 일치만으로 계산하는 키워드
    // 점수 - rewriteForSearch가 이미 의학 용어로 정규화해주므로 활용형/동의어는 대부분 그쪽에서
    // 흡수된다. RRF는 절대값이 아니라 이 점수로 정렬했을 때의 순위만 쓰므로 가중치 스케일 자체는
    // 중요하지 않고, "질병명 완전일치 > 부분일치 > 본문 커버리지 > 섹션명 커버리지" 순서만 지킨다.
    private double keywordScore(Document doc, String query, List<String> queryTokens) {
        String disease = String.valueOf(doc.getMetadata().get("disease"));
        String section = String.valueOf(doc.getMetadata().get("section"));
        String text = doc.getText();
        String trimmedQuery = query.trim();

        double score = 0.0;
        if (trimmedQuery.equalsIgnoreCase(disease) || queryTokens.stream().anyMatch(t -> t.equalsIgnoreCase(disease))) {
            score += EXACT_MATCH_KEYWORD_SCORE;
        } else if (disease.contains(trimmedQuery) || trimmedQuery.contains(disease)) {
            score += 1.5;
        }

        if (!queryTokens.isEmpty()) {
            long textHits = queryTokens.stream().filter(text::contains).count();
            score += 1.0 * textHits / queryTokens.size();

            long sectionHits = queryTokens.stream().filter(section::contains).count();
            score += 0.5 * sectionHits / queryTokens.size();
        }

        return score;
    }

    private List<String> tokenize(String query) {
        return Arrays.stream(query.trim().split("\\s+"))
                .filter(t -> t.length() >= 2)
                .toList();
    }

    // LLM 프롬프트에 넣을 "[번호] (질병명) 본문" 형태의 컨텍스트 문자열. 번호는 1부터 시작하고,
    // 이 번호가 그대로 인용 근거 번호(sourceIndexes)로 쓰인다.
    public String buildContext(List<Document> docs) {
        return IntStream.rangeClosed(1, docs.size())
                .mapToObj(i -> "[%d] (%s) %s".formatted(
                        i, docs.get(i - 1).getMetadata().get("disease"), docs.get(i - 1).getText()))
                .collect(Collectors.joining("\n\n"));
    }
}
