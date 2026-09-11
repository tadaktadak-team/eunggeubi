package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.Svc;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

// KDCA 응답 하나(질병 하나)를 Spring AI Document 여러 개(섹션 단위 청크)로 변환한다.
// 본문이 비었거나 이미지 URL(http로 시작)인 섹션, EXCLUDED_SECTIONS에 해당하는 섹션은 제외한다.
@Component
public class HealthInfoDocumentMapper {

    private static final String SOURCE = "질병관리청 국가건강정보포털";

    // 증상 상담과 무관하거나(참고문헌/연관 주제어) 행정·통계성이라 개인 증상 판단에 안 쓰이는(신고
    // 대상 및 방법/지원체계/통계) 섹션은 검색 노이즈만 늘려서 인덱싱에서 뺀다. 실제 KDCA 응답에 나온
    // 섹션명 40개를 다 확인하고 고른 목록 - 새 섹션명이 나오면 이 목록에 없으니 기본은 포함된다.
    private static final Set<String> EXCLUDED_SECTIONS = Set.of(
            "참고문헌", "연관 주제어", "연관주제", "신고 대상 및 방법", "지원체계",
            "통계", "역학 및 통계", "검사 적응증 및 금기증"
    );

    // OpenAI 임베딩 입력 토큰 한도(8191)를 넉넉히 밑도는 안전 마진.
    // 실제로 "자주하는 질문"(9023자) 같은 섹션이 존재해서 그대로 넣으면 통째로 실패하는 게 확인됨
    // -> 이 길이를 넘으면 여러 청크로 쪼갠다.
    private static final int MAX_CHUNK_CHARS = 3000;

    public List<Document> toDocuments(KdcaHealthInfoResponse response) {
        List<Document> documents = new ArrayList<>();
        Svc svc = response.getSvc();
        if (svc == null || svc.getSections() == null) {
            return documents;
        }

        for (Svc.Section section : svc.getSections()) {
            String content = section.getContent();
            if (content == null || content.isBlank() || content.trim().startsWith("http")) {
                continue;
            }
            if (EXCLUDED_SECTIONS.contains(section.getName())) {
                continue;
            }

            List<String> pieces = TextChunker.split(content.trim(), MAX_CHUNK_CHARS);
            for (int i = 0; i < pieces.size(); i++) {
                String sectionName = pieces.size() > 1
                        ? "%s (%d/%d)".formatted(section.getName(), i + 1, pieces.size())
                        : section.getName();
                Map<String, Object> metadata = Map.of(
                        "source", SOURCE,
                        "disease", svc.getDiseaseName(),
                        "section", sectionName,
                        "cntntsSn", svc.getCntntsSn()
                );
                String id = chunkId(svc.getCntntsSn(), section.getName(), i);
                documents.add(new Document(id, pieces.get(i), metadata));
            }
        }
        return documents;
    }

    // 질병(cntntsSn)+섹션명+청크 순번으로 고정된 ID를 만든다 - ID를 안 주면 Spring AI가 매번 랜덤 UUID를
    // 생성해서, 리셋 없이 같은 배치를 두 번 돌리면 같은 내용이 중복으로 쌓인다. Qdrant 포인트 ID는
    // UUID/정수만 허용해서 문자열 키를 그대로 못 쓰고 UUID로 해시한다.
    private String chunkId(String cntntsSn, String sectionName, int chunkIndex) {
        String key = "%s|%s|%d".formatted(cntntsSn, sectionName, chunkIndex);
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
