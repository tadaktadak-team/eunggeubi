package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotNull LocalDate birthDate,   // "1990-01-01" 형식
        @NotNull Gender gender,         // MALE / FEMALE / NONE
        String address                  // 선택
) {
}