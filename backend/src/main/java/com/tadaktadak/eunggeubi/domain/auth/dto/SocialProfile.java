package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import java.time.LocalDate;

/**
 * 소셜 제공자(네이버/카카오)에서 받아온 프로필을 우리 서비스 형태로 정규화한 값.
 * 제공자마다 응답 형식이 달라서, 각 OAuth 클라이언트가 이 형태로 변환해 넘긴다.
 */
public record SocialProfile(
        String providerUserId,  // 제공자가 주는 고유 식별자 (연동 키)
        String email,           // 계정 이메일 (우리 서비스 로그인 식별자)
        String name,            // 이름
        String phone,           // 휴대전화 (하이픈 제거된 형태), 없으면 null
        LocalDate birthDate,    // 생년월일, 없으면 null
        Gender gender           // 성별, 없으면 NONE
) {
}