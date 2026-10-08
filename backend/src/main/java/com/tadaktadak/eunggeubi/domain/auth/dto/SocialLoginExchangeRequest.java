package com.tadaktadak.eunggeubi.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 소셜 콜백에서 받은 1회용 loginTicket 을 실제 토큰으로 교환할 때 쓰는 요청 본문.
public record SocialLoginExchangeRequest(
        @NotBlank String ticket
) {
}