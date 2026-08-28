package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.KdcaHealthInfoResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.Svc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

// KDCA 응답 하나(질병 하나)를 Spring AI Document 여러 개(섹션 단위 청크)로 변환한다.
// 본문이 비었거나 이미지 URL(http로 시작)인 섹션은 임베딩 대상에서 제외한다.
@Component
public class HealthInfoDocumentMapper {

    private static final String SOURCE = "질병관리청 국가건강정보포털";

    // OpenAI 임베딩 입력 토큰 한도(8191)를 넉넉히 밑도는 안전 마진.
    // 실제로 "자주하는 질문"(9023자), "참고문헌"(14592자) 같은 섹션이 존재해서
    // 그대로 넣으면 통째로 실패하는 게 확인됨 -> 이 길이를 넘으면 여러 청크로 쪼갠다.
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
                documents.add(new Document(pieces.get(i), metadata));
            }
        }
        return documents;
    }
}
