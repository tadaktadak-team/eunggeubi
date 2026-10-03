package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.PhoneVerification;
import com.tadaktadak.eunggeubi.domain.auth.entity.Purpose;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    // 특정 전화번호+목적의 가장 최근 인증건 조회 (인증코드 확인용)
    Optional<PhoneVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(String phone, Purpose purpose);
    // 같은 번호의 가장 최근 발송 (목적 무관) - 재발송 쿨다운 확인용.
    // 목적을 SIGNUP/FIND_ID 로 바꿔가며 쿨다운을 우회하는 걸 막으려고 purpose 를 보지 않는다.
    Optional<PhoneVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);

    // 특정 시각 이후 이 번호로 발송한 건수 - 번호당 일일 상한 확인용
    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime after);

    // 특정 시각 이후 전체 발송 건수 - 서비스 전체 일일 상한(= 문자 요금 상한) 확인용
    long countByCreatedAtAfter(LocalDateTime after);

}