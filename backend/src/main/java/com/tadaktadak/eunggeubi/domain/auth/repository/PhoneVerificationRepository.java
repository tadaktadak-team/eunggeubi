package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.PhoneVerification;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    // 특정 전화번호+목적의 가장 최근 인증건 조회 (인증코드 확인용)
    Optional<PhoneVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, Purpose purpose);
}