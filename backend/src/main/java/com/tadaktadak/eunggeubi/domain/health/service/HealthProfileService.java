package com.tadaktadak.eunggeubi.domain.health.service;

import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileRequest;
import com.tadaktadak.eunggeubi.domain.health.dto.MedicationItem;
import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileResponse;
import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
        String allergies = toCsv(req.allergies());

        // 새 앱은 약 이름+번호(medicationItems)를, 예전 앱은 이름만(medications)을 보낸다
        List<MedicationItem> meds = MedicationItem.distinct(
                req.medicationItems() != null ? req.medicationItems() : MedicationItem.fromNames(req.medications()));
        String medications = MedicationItem.toCsv(meds);
        String medicationItems = meds.isEmpty() ? null : MedicationItem.toJson(meds);

        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .map(p -> {
                    p.update(req.bloodType(), diseases, medications, medicationItems, allergies);
                    return p; // 변경 감지로 UPDATE
                })
                .orElseGet(() -> healthProfileRepository.save(
                        HealthProfile.builder()
                                .userId(userId)
                                .bloodType(req.bloodType())
                                .diseases(diseases)
                                .medications(medications)
                                .medicationItems(medicationItems)
                                .allergies(allergies)
                                .build()
                ));

        return HealthProfileResponse.from(profile);
    }

    // 콤마로 이어 저장하므로 항목 안의 콤마("땅콩, 호두")는 미리 나눠 둔다. 그래야 다시 읽을 때와 결과가 같다
    private String toCsv(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        String csv = items.stream()
                .filter(Objects::nonNull)   // ["고혈압", null] 같은 요청이 서버 오류(500)가 되지 않게
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        return csv.isEmpty() ? null : csv;
    }
}