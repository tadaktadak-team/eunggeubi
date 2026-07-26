package com.tadaktadak.eunggeubi.domain.auth.controller;

import com.tadaktadak.eunggeubi.domain.auth.dto.SendCodeRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.VerifyCodeRequest;
import com.tadaktadak.eunggeubi.domain.auth.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody SendCodeRequest request) {
        phoneVerificationService.sendCode(request.phone(), request.purpose());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@Valid @RequestBody VerifyCodeRequest request) {
        phoneVerificationService.verifyCode(request.phone(), request.purpose(), request.code());
        return ResponseEntity.ok().build();
    }
}