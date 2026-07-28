package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FindEmailRequest(
        @NotBlank String name,
        @NotBlank String phone
) {
}