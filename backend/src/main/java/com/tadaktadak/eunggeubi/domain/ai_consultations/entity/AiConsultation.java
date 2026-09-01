package com.tadaktadak.eunggeubi.domain.ai_consultations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// AI 상담 대화 한 턴(사용자 메시지 또는 AI 응답)을 한 행으로 저장한다. 같은 session_id를 가진 행들을
// created_at 순으로 모으면 상담 세션 전체 대화가 된다.
// updated_at이 필요 없는 테이블이라 BaseTimeEntity를 상속하지 않고 created_at을 직접 관리한다
// (Guardian 엔티티와 같은 패턴).
@Getter
@Entity
@Table(name = "ai_consultations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id") // 게스트는 null
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 50)
    private String sessionId;

    @Column(name = "is_session_root", nullable = false)
    private boolean sessionRoot;

    @Column(name = "guest_code", length = 20) // 로그인 사용자는 null
    private String guestCode;

    @Column(name = "symptom_keyword", length = 100)
    private String symptomKeyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private SenderType senderType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_regenerated", nullable = false)
    private boolean regenerated;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "based_on_response_id") // 재생성인 경우, 재생성 대상이 된 원본 AI 응답의 id
    private Long basedOnResponseId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private AiConsultation(Long userId, String sessionId, boolean sessionRoot, String guestCode,
                            String symptomKeyword, SenderType senderType, String content,
                            boolean regenerated, Long parentId, Long basedOnResponseId) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.sessionRoot = sessionRoot;
        this.guestCode = guestCode;
        this.symptomKeyword = symptomKeyword;
        this.senderType = senderType;
        this.content = content;
        this.regenerated = regenerated;
        this.parentId = parentId;
        this.basedOnResponseId = basedOnResponseId;
        this.createdAt = LocalDateTime.now();
    }

    // userId(로그인)와 guestCode(비로그인) 둘 중 하나라도 일치하면 본인 세션으로 본다.
    public boolean isOwnedBy(Long requesterUserId, String requesterGuestCode) {
        if (this.userId != null) {
            return this.userId.equals(requesterUserId);
        }
        return this.guestCode != null && this.guestCode.equals(requesterGuestCode);
    }
}
