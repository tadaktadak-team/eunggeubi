package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.ChecklistResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistResponseRepository extends JpaRepository<ChecklistResponse, Long> {
    Optional<ChecklistResponse> findTopByChecklistIdOrderByCreatedAtDesc(Long checklistId);
}
