package com.tadaktadak.eunggeubi.domain.health.entity;

import com.tadaktadak.eunggeubi.global.common.BaseTimeEntity;
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

@Getter
@Entity
@Table(name = "health_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true) // 회원당 1개 (1:1)
    private Long userId;

    @Column(name = "blood_type", length = 10)
    private String bloodType;

    @Column(columnDefinition = "TEXT") //콤마 구분
    private String diseases;

    @Column(columnDefinition = "TEXT") //콤마 구분
    private String allergies;

    @Column(columnDefinition = "TEXT") //콤마 구분
    private String medications;

    @Builder
    private HealthProfile(Long userId, String bloodType, String diseases,
                          String allergies, String medications) {
        this.userId = userId;
        this.bloodType = bloodType;
        this.diseases = diseases;
        this.allergies = allergies;
        this.medications = medications;
    }

    // 건강 프로필 수정 (변경 감지로 반영)
    public void update(String bloodType, String diseases, String allergies, String medications) {
        this.bloodType = bloodType;
        this.diseases = diseases;
        this.allergies = allergies;
        this.medications = medications;
    }
}