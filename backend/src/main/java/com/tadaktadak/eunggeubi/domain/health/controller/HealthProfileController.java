package com.tadaktadak.eunggeubi.domain.health.controller;

import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileRequest;
import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileResponse;
import com.tadaktadak.eunggeubi.domain.health.dto.MedicationSearchItem;
import com.tadaktadak.eunggeubi.domain.health.service.MedicationSearchService;
import com.tadaktadak.eunggeubi.domain.health.service.HealthProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService healthProfileService;
    private final MedicationSearchService medicationSearchService;

    @GetMapping
    public HealthProfileResponse getProfile(@AuthenticationPrincipal Long userId) {
        return healthProfileService.getProfile(userId);
    }

    @GetMapping("/medications/search")
    public List<MedicationSearchItem> searchMedications(
            @RequestParam
            @NotBlank(message = "검색어를 입력해주세요.")
            @Size(max = 50, message = "검색어는 50자 이내로 입력해주세요.") String keyword) {
        return medicationSearchService.search(keyword);
    }

    @PutMapping
    public HealthProfileResponse saveProfile(@AuthenticationPrincipal Long userId,
                                             @Valid @RequestBody HealthProfileRequest request) {
        return healthProfileService.saveProfile(userId, request);
    }
}