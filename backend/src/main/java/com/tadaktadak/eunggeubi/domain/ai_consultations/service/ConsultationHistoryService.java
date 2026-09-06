package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationDetailResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationSummaryResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//마이페이지 상담 이력 조회
@Service
@RequiredArgsConstructor
public class ConsultationHistoryService {

    private final AiConsultationRepository aiConsultationRepository;

    @Transactional(readOnly = true)
    public List<ConsultationSummaryResponse> getMyConsultations(Long userId) {
        return aiConsultationRepository.findByUserIdAndSessionRootTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(ConsultationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsultationDetailResponse getMyConsultationDetail(Long userId, String sessionId) {
        List<AiConsultation> messages =
                aiConsultationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("상담 내역을 찾을 수 없습니다.");
        }

        // sessionId는 클라이언트가 보내는 값이므로, 남의 세션을 조회하지 못하게 소유자를 확인(비회원은 대조X)
        if (!messages.get(0).isOwnedBy(userId, null)) {
            throw new IllegalArgumentException("본인 상담 내역이 아닙니다.");
        }

        return ConsultationDetailResponse.from(sessionId, messages);
    }
}