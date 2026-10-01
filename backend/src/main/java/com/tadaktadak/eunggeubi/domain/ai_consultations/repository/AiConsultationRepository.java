package com.tadaktadak.eunggeubi.domain.ai_consultations.repository;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.SenderType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiConsultationRepository extends JpaRepository<AiConsultation, Long> {

    // 세션 전체 대화를 시간순으로 (재생성 시 원 증상 텍스트를 찾을 때 사용)
    List<AiConsultation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    //마이페이지 상담 이력 목록(세션의 첫 메세지만)
    List<AiConsultation> findByUserIdAndSessionRootTrueOrderByCreatedAtDesc(Long userId);

    // 비회원 사용 횟수 제한용 - 이 guestCode로 보낸 사용자 메시지 수
    long countByGuestCodeAndSenderType(String guestCode, SenderType senderType);

    // 이 답변을 기반으로 재생성한 적이 있는지 - 비회원 재생성 1회 제한용(ConsultationRegenerationService)
    boolean existsByBasedOnResponseId(Long basedOnResponseId);

    // 로그인 후 사용자가 동의하면 비회원 기록을 그 계정으로 옮긴다(ConsultationHistoryService). guest_code는 비워서 이후 게스트 요청으로는 접근 불가.
    @Modifying
    @Query("update AiConsultation a set a.userId = :userId, a.guestCode = null "
            + "where a.guestCode = :guestCode and a.userId is null")
    int claimGuestConsultations(@Param("userId") Long userId, @Param("guestCode") String guestCode);
}
