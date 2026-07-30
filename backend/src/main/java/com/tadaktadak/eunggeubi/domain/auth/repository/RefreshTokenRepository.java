package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 토큰 재발급 시 저장된 리프레시 토큰 조회
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    // 특정 회원의 아직 폐기되지 않은 refresh 토큰 전체 (탈퇴 시 일괄 폐기용)
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}