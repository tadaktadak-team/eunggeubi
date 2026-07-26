package com.tadaktadak.eunggeubi.domain.auth.repository;

import com.tadaktadak.eunggeubi.domain.auth.entity.Provider;
import com.tadaktadak.eunggeubi.domain.auth.entity.SocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    // 소셜 로그인 시 (제공자 + 소셜회원id)로 이미 연동된 계정 조회
    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}