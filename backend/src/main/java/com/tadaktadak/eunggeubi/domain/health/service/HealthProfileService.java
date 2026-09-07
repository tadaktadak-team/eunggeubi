package com.tadaktadak.eunggeubi.domain.health.service;

import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileRequest;
import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileResponse;
import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;

    @Transactional(readOnly = true)
    public HealthProfileResponse getProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .map(HealthProfileResponse::from)
                .orElseGet(HealthProfileResponse::empty);
    }

    @Transactional
    public HealthProfileResponse saveProfile(Long userId, HealthProfileRequest req) {
        String diseases = toCsv(req.diseases());
        String medications = toCsv(req.medications());
        String allergies = toCsv(req.allergies());

        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .map(p -> {
                    p.update(req.bloodType(), diseases, medications, allergies);
                    return p; // 변경 감지로 UPDATE
                })
                .orElseGet(() -> healthProfileRepository.save(
                        HealthProfile.builder()
                                .userId(userId)
                                .bloodType(req.bloodType())
                                .diseases(diseases)
                                .medications(medications)
                                .allergies(allergies)
                                .build()
                ));

        return HealthProfileResponse.from(profile);
    }

    private String toCsv(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}