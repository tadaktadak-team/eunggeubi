package com.tadaktadak.eunggeubi.domain.ai_consultations.controller;

import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationRequest;
import com.tadaktadak.eunggeubi.domain.ai_consultations.dto.ConsultationResponse;
import com.tadaktadak.eunggeubi.domain.ai_consultations.service.AiConsultationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-consultations")
@RequiredArgsConstructor
public class AiConsultationController {

    private final AiConsultationService aiConsultationService;

    @PostMapping
    public ResponseEntity<ConsultationResponse> consult(@Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.ok(aiConsultationService.consult(request.query()));
    }
}
