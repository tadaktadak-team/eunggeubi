package com.tadaktadak.eunggeubi.domain.user.entity;

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
@Table(name = "guardians")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relationship relationship;

    @Column(name = "notify_enabled", nullable = false)
    private boolean notifyEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Guardian(Long userId, String name, String phone, Relationship relationship,
                     boolean notifyEnabled, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.notifyEnabled = notifyEnabled;
        this.createdAt = createdAt;
    }

    // 보호자 정보 수정 (변경 감지로 반영)
    public void update(String name, String phone, Relationship relationship, boolean notifyEnabled) {
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.notifyEnabled = notifyEnabled;
    }
}