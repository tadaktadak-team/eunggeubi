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

    // 보호자가 문자 링크 클릭 → 동의 확정 (브라우저에 안내 문구 표시)
    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam String token) {
        guardianConsentService.confirmConsent(token);
        return ResponseEntity.ok("보호자 동의가 완료되었습니다. 이제 앱에서 로그인할 수 있어요.");
    }

    // 동의 완료 여부 확인 (앱의 '발송 대기' 화면용)
    @GetMapping("/status")
    public ResponseEntity<ConsentStatusResponse> status(@RequestParam Long userId) {
        return ResponseEntity.ok(guardianConsentService.getStatus(userId));
    }
}