package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 토큰 재발급 시 저장된 리프레시 토큰 조회
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}