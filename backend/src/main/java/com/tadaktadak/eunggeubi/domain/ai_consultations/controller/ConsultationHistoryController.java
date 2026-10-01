package com.tadaktadak.eunggeubi.domain.ai_consultations.controller;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationDetailResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationSummaryResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ConsultationHistoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // 로그인 직후 "이 기기의 비회원 상담 기록을 가져올까요?"에 동의했을 때 - 그 guestCode의 기록을 내 계정으로.
    // 가입 화면이 아니라 로그인 시점에 묻는 건 이메일/소셜/기존 계정 로그인을 한 곳에서 처리하려고.
    @PostMapping("/claim-guest")
    public ClaimGuestResponse claimGuest(@AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody ClaimGuestRequest request) {
        return new ClaimGuestResponse(consultationHistoryService.claimGuestConsultations(userId, request.guestCode()));
    }

    public record ClaimGuestRequest(@NotBlank String guestCode) {}

    public record ClaimGuestResponse(int claimed) {}

    //상담 내역 상세 보기
    @GetMapping("/{sessionId}")
    public ConsultationDetailResponse getMyConsultationDetail(@AuthenticationPrincipal Long userId,
                                                              @PathVariable String sessionId) {
        return consultationHistoryService.getMyConsultationDetail(userId, sessionId);
    }
}