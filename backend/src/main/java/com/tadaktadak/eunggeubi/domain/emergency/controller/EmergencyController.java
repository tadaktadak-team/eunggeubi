package com.tadaktadak.eunggeubi.domain.emergency.controller;

import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertRequest;
import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertResponse;
import com.tadaktadak.eunggeubi.domain.emergency.service.EmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;

    @PostMapping("/alert")
    public ResponseEntity<EmergencyAlertResponse> alert(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EmergencyAlertRequest request) {
        return ResponseEntity.ok(emergencyService.sendAlert(userId, request));
    }
}