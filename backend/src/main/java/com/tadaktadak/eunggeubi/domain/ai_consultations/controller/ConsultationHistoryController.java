package com.tadaktadak.eunggeubi.domain.ai_consultations.controller;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationDetailResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationSummaryResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ConsultationHistoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/users/me/consultations") //ai 상담의 경우 비회원도 가능하기 때문에, 인증이 강제되는 users 아래로 매핑
@RequiredArgsConstructor
public class ConsultationHistoryController {

    private final ConsultationHistoryService consultationHistoryService;

    //최근 상담 내역 조회
    @GetMapping
    public List<ConsultationSummaryResponse> getMyConsultations(@AuthenticationPrincipal Long userId) {
        return consultationHistoryService.getMyConsultations(userId);
    }

    //상담 내역 상세 보기
    @GetMapping("/{sessionId}")
    public ConsultationDetailResponse getMyConsultationDetail(@AuthenticationPrincipal Long userId,
                                                              @PathVariable String sessionId) {
        return consultationHistoryService.getMyConsultationDetail(userId, sessionId);
    }
}