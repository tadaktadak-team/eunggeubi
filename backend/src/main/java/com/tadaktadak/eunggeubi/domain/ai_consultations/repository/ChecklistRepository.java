package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.Checklist;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    // 한 상담(consultation)에 체크리스트가 여러 번 생성될 수 있어 가장 최근 것만 가져온다.
    Optional<Checklist> findTopByConsultationIdOrderByCreatedAtDesc(Long consultationId);
}
