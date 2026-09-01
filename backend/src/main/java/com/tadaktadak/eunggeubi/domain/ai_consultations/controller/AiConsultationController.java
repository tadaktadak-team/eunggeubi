package com.tadaktadak.eunggeubi.domain.ai_consultations.controller;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ChecklistDto;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.GenerateChecklistRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.RegenerateResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.SubmitChecklistResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.AiConsultationService;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ChecklistService;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.ConsultationRegenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 이 컨트롤러의 전체 경로(/api/ai-consultations/**)는 SecurityConfig에서 permitAll이라 비로그인
// 사용자도 쓸 수 있다. userId는 로그인 상태면 채워지고, 게스트면 null로 들어온다
// (JwtAuthenticationFilter가 토큰이 없어도 필터체인은 통과시키기 때문 - 401을 던지지 않음).
@RestController
@RequestMapping("/api/ai-consultations")
@RequiredArgsConstructor
public class AiConsultationController {

    private final AiConsultationService aiConsultationService;
    private final ChecklistService checklistService;
    private final ConsultationRegenerationService consultationRegenerationService;

    @PostMapping
    public ResponseEntity<ConsultationResponse> consult(@AuthenticationPrincipal Long userId,
                                                          @Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.ok(aiConsultationService.consult(request, userId));
    }

    @PostMapping("/{id}/checklist")
    public ResponseEntity<ChecklistDto> generateChecklist(@AuthenticationPrincipal Long userId,
                                                            @PathVariable Long id,
                                                            @RequestBody(required = false) GenerateChecklistRequest request) {
        String guestCode = request != null ? request.guestCode() : null;
        return ResponseEntity.ok(checklistService.generate(id, userId, guestCode));
    }

    @PostMapping("/{id}/checklist/submit")
    public ResponseEntity<SubmitChecklistResponse> submitChecklist(@AuthenticationPrincipal Long userId,
                                                                     @PathVariable Long id,
                                                                     @Valid @RequestBody SubmitChecklistRequest request) {
        return ResponseEntity.ok(checklistService.submit(id, request, userId));
    }

    @PostMapping("/{id}/regenerate")
    public ResponseEntity<RegenerateResponse> regenerate(@AuthenticationPrincipal Long userId,
                                                           @PathVariable Long id,
                                                           @RequestBody(required = false) RegenerateRequest request) {
        String guestCode = request != null ? request.guestCode() : null;
        return ResponseEntity.ok(consultationRegenerationService.regenerate(id, userId, guestCode));
    }
}
