package com.tadaktadak.eunggeubi.domain.ai_consultations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 메시지(ai_consultations, sender_type=AI) 한 건이 실제로 인용한 참고자료 한 건.
// 검색은 됐지만 답변에서 인용되지 않은 자료는 저장하지 않는다 (AiConsultationService의 필터링 원칙 그대로).
@Getter
@Entity
@Table(name = "reference_sources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consultation_id", nullable = false)
    private Long consultationId;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "source_url", length = 255) // KDCA 문서엔 별도 URL이 없어 지금은 항상 null
    private String sourceUrl;

    @Column(name = "relevance_note", columnDefinition = "TEXT")
    private String relevanceNote;

    @Builder
    private ReferenceSource(Long consultationId, String sourceName, String sourceUrl, String relevanceNote) {
        this.consultationId = consultationId;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.relevanceNote = relevanceNote;
    }
}
