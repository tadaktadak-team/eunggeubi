package com.tadaktadak.eunggeubi.domain.auth.controller;

import com.tadaktadak.eunggeubi.domain.auth.dto.FindEmailRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.FindEmailResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.ResetPasswordRequest;
import com.tadaktadak.eunggeubi.domain.auth.service.AccountRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/find-email")
    public ResponseEntity<FindEmailResponse> findEmail(@Valid @RequestBody FindEmailRequest request) {
        String maskedEmail = accountRecoveryService.findEmail(request.name(), request.phone());
        return ResponseEntity.ok(new FindEmailResponse(maskedEmail));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPassword(request.email(), request.phone(), request.newPassword());
        return ResponseEntity.noContent().build(); // 204
    }
}
