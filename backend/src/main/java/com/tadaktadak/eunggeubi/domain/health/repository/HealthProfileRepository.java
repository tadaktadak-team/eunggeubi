package com.tadaktadak.eunggeubi.domain.health.repository;

import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
    Optional<HealthProfile> findByUserId(Long userId);
}