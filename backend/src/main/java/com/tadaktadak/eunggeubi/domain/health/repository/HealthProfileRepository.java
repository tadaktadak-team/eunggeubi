package com.tadaktadak.eunggeubi.domain.health.repository;

import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
    Optional<HealthProfile> findByUserId(Long userId);

    // 회원 탈퇴 때 건강 프로필(혈액형·병명·복용약·알레르기)을 지운다
    void deleteByUserId(Long userId);
}