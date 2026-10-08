package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.ConsentStatus;
import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, Long> {

    // 보호자가 동의 링크 클릭 시, 토큰으로 동의 요청 조회
    Optional<GuardianConsent> findByConsentToken(String consentToken);

    Optional<GuardianConsent> findTopByGuardianIdOrderBySentAtDesc(Long guardianId);

    List<GuardianConsent> findByGuardianIdAndStatus(Long guardianId, ConsentStatus status);

    long countByGuardianIdAndSentAtAfter(Long guardianId, LocalDateTime since);

    long countByUserIdAndSentAtAfter(Long userId, LocalDateTime since);

    long countByPhoneAndSentAtAfter(String phone, LocalDateTime since);

    // 회원 탈퇴 때 동의 기록을 지운다. 보호자를 따로 지운 뒤 남은 기록은 userId 로,
    // userId 칸이 생기기 전에 만든 예전 기록은 보호자 번호(guardianId)로 찾는다
    void deleteByUserId(Long userId);

    void deleteByGuardianIdIn(Collection<Long> guardianIds);
}
