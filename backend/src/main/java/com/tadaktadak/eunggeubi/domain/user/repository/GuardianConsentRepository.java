package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.GuardianConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, Long> {

    // 보호자가 동의 링크 클릭 시, 토큰으로 동의 요청 조회
    Optional<GuardianConsent> findByConsentToken(String consentToken);
}