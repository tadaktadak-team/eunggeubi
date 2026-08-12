package com.tadaktadak.eunggeubi.domain.health.controller;

import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileRequest;
import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileResponse;
import com.tadaktadak.eunggeubi.domain.health.service.HealthProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    @GetMapping
    public HealthProfileResponse getProfile(@AuthenticationPrincipal Long userId) {
        return healthProfileService.getProfile(userId);
    }

    @PutMapping
    public HealthProfileResponse saveProfile(@AuthenticationPrincipal Long userId,
                                             @RequestBody HealthProfileRequest request) {
        return healthProfileService.saveProfile(userId, request);
    }
}