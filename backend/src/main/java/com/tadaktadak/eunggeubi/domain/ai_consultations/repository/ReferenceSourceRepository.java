package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ReferenceSource;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceSourceRepository extends JpaRepository<ReferenceSource, Long> {
    List<ReferenceSource> findByConsultationId(Long consultationId);
}
