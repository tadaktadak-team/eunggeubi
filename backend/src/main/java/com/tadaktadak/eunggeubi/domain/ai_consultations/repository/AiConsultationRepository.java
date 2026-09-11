package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConsultationRepository extends JpaRepository<AiConsultation, Long> {

    // 세션 전체 대화를 시간순으로 (재생성 시 원 증상 텍스트를 찾을 때 사용)
    List<AiConsultation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // 세션에서 가장 최근 메시지 1건 (예: 최신 AI 응답)
    Optional<AiConsultation> findTopBySessionIdAndSenderTypeOrderByCreatedAtDesc(String sessionId, SenderType senderType);

    //마이페이지 상담 이력 목록(세션의 첫 메세지만)
    List<AiConsultation> findByUserIdAndSessionRootTrueOrderByCreatedAtDesc(Long userId);
}
