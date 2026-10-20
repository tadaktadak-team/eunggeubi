package com.tadaktadak.eunggeubi.domain.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HealthProfileRequest(
        @Size(max = 10, message = "혈액형이 올바르지 않습니다.") String bloodType, //혈액형
        @Size(max = 20, message = "병명은 최대 20개까지 등록할 수 있습니다.")
        List<@Size(max = 50, message = "항목은 50자 이내로 입력해주세요.") String> diseases, //병명
        @Size(max = 20, message = "약명은 최대 20개까지 등록할 수 있습니다.")
        List<@Size(max = 200, message = "약 이름은 200자 이내로 입력해주세요.") String> medications, //약명(이름만, 예전 앱 호환)
        @Valid
        @Size(max = 20, message = "약명은 최대 20개까지 등록할 수 있습니다.")
        List<MedicationItem> medicationItems, //약명+품목번호. 있으면 medications보다 우선한다
        @Size(max = 20, message = "알레르기는 최대 20개까지 등록할 수 있습니다.")
        List<@Size(max = 50, message = "항목은 50자 이내로 입력해주세요.") String> allergies //알레르기
) {
}
