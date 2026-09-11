package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.RefreshToken;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 토큰 재발급 시 저장된 리프레시 토큰 조회
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 특정 회원의 아직 살아있는 refresh 토큰 전체 (탈퇴/비밀번호 변경 시 일괄 폐기용)
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(Long userId);
}
