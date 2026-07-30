package com.tadaktadak.eunggeubi.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank String password
) {
}