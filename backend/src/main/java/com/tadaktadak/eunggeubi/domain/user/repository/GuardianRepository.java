package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    // 특정 회원에 등록된 보호자 목록 조회
    List<Guardian> findByUserId(Long userId);

    // 긴급 알림 발송 대상: 수신 ON + 보호자가 문자로 동의한 사람만
    List<Guardian> findByUserIdAndNotifyEnabledTrueAndVerifiedAtIsNotNull(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndPhone(Long userId, String phone);

    boolean existsByUserIdAndPhoneAndIdNot(Long userId, String phone, Long id);
}
