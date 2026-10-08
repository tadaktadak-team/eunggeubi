package com.tadaktadak.eunggeubi.domain.user.repository;

import com.tadaktadak.eunggeubi.domain.user.entity.TermsAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {

    // 특정 회원의 약관 동의 내역 전체 조회
    List<TermsAgreement> findByUserId(Long userId);
}