package com.tadaktadak.eunggeubi.domain.user.entity;

import com.tadaktadak.eunggeubi.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 255)
    private String password; // 소셜 로그인 회원은 비밀번호가 없을 수 있음

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Builder
    private User(String email, String password, String name, String phone,
                 LocalDate birthDate, Gender gender, String address, UserStatus status) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
        this.status = status;
    }

    // 비밀번호 변경 (재설정 시 사용)
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
    // 회원 탈퇴 (상태를 WITHDRAWN으로 — 소프트 삭제)
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    // 회원 정보 수정 (이메일·비밀번호는 변경 불가)
    public void updateProfile(String name, String phone, LocalDate birthDate,
                              Gender gender, String address) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
    }
    // 보호자 동의 완료 → 계정 활성화
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

}