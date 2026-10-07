package com.tadaktadak.eunggeubi.domain.auth.controller;

import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentStatusResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianConsentResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.auth.service.GuardianConsentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/guardian")
@RequiredArgsConstructor
public class GuardianConsentController {

    private final GuardianConsentService guardianConsentService;

    // 보호자 정보 입력 + 동의 문자 발송
    @PostMapping("/request")
    public ResponseEntity<GuardianConsentResponse> request(@Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(guardianConsentService.requestConsent(request));
    }


    // 동의 완료 여부 확인 (앱의 '발송 대기' 화면용)
    @GetMapping("/status")
    public ResponseEntity<ConsentStatusResponse> status(@RequestParam String consentToken) {
        return ResponseEntity.ok(guardianConsentService.getStatus(consentToken));
    }
}