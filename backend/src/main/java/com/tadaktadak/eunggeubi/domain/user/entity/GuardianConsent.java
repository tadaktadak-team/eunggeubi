package com.tadaktadak.eunggeubi.domain.user.entity;

import com.tadaktadak.eunggeubi.global.common.MessageType;
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

@Getter
@Entity
@Table(name = "guardian_consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    @Column(name = "consent_token", nullable = false, length = 64)
    private String consentToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentStatus status;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 10)
    private MessageType messageType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    private GuardianConsent(Long guardianId, String consentToken, ConsentStatus status,
                            String providerMessageId, MessageType messageType,
                            LocalDateTime sentAt, LocalDateTime confirmedAt, LocalDateTime expiresAt) {
        this.guardianId = guardianId;
        this.consentToken = consentToken;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.messageType = messageType;
        this.sentAt = sentAt;
        this.confirmedAt = confirmedAt;
        this.expiresAt = expiresAt;
    }
}