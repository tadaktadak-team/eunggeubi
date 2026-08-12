package com.tadaktadak.eunggeubi.domain.health.dto;

import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import java.util.Arrays;
import java.util.List;

// 프론트로 보내는 응답: DB의 콤마 문자열을 다시 리스트로 쪼개서 전달
public record HealthProfileResponse(
        String bloodType,
        List<String> diseases,
        List<String> allergies,
        List<String> medications
) {
    public static HealthProfileResponse from(HealthProfile p) {
        return new HealthProfileResponse(
                p.getBloodType(),
                toList(p.getDiseases()),
                toList(p.getAllergies()),
                toList(p.getMedications())
        );
    }

    //프로필이 없는 경우 빈 프로필 반환
    public static HealthProfileResponse empty() {
        return new HealthProfileResponse(null, List.of(), List.of(), List.of());
    }

    //콤마로 구분해둔 값들을 리스트에 담기
    private static List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}