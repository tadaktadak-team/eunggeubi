package com.tadaktadak.eunggeubi.domain.health.dto;

import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import java.util.Arrays;
import java.util.List;

// 프론트로 보내는 응답: DB의 콤마 문자열을 다시 리스트로 쪼개서 전달
public record HealthProfileResponse(
        String bloodType,
        List<String> diseases,
        List<String> medications,
        List<MedicationItem> medicationItems,
        List<String> allergies
) {
    public static HealthProfileResponse from(HealthProfile p) {
        // 번호까지 저장된 값이 있으면 그걸 쓰고, 예전에 이름만 저장한 프로필(또는 읽을 수 없는 값)은 번호 없이 복원한다
        List<MedicationItem> medicationItems = MedicationItem.fromStored(p.getMedicationItems(), p.getMedications());
        return new HealthProfileResponse(
                p.getBloodType(),
                toList(p.getDiseases()),
                medicationItems.stream().map(MedicationItem::name).toList(),
                medicationItems,
                toList(p.getAllergies())
        );
    }

    //프로필이 없는 경우 빈 프로필 반환
    public static HealthProfileResponse empty() {
        return new HealthProfileResponse(null, List.of(), List.of(), List.of(), List.of());
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
