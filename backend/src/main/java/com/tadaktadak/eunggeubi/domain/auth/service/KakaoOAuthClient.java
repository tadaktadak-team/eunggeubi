package com.tadaktadak.eunggeubi.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialProfile;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오 로그인 OAuth 처리기.
 * authorize URL 생성 → 인가코드로 토큰 교환(POST) → 토큰으로 프로필 조회 → SocialProfile 로 정규화.
 * 응답 스펙: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
 */
@Slf4j
@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizeUri;
    private final String tokenUri;
    private final String userInfoUri;

    public KakaoOAuthClient(
            RestTemplate restTemplate,
            @Value("${oauth.kakao.client-id:}") String clientId,
            @Value("${oauth.kakao.client-secret:}") String clientSecret,
            @Value("${oauth.kakao.redirect-uri:}") String redirectUri,
            @Value("${oauth.kakao.authorize-uri:}") String authorizeUri,
            @Value("${oauth.kakao.token-uri:}") String tokenUri,
            @Value("${oauth.kakao.userinfo-uri:}") String userInfoUri) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizeUri = authorizeUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    // 1) 카카오 로그인 페이지 URL
    public String buildAuthorizeUrl(String state) {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException("카카오 client-id(REST API 키)가 설정되지 않았습니다. (oauth.kakao.client-id)");
        }
        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();
    }

    // 2)+3) 인가코드 → 액세스토큰 → 프로필
    public SocialProfile fetchProfile(String code) {
        String accessToken = requestAccessToken(code);
        return requestProfile(accessToken);
    }

    // 카카오 토큰 교환: POST + x-www-form-urlencoded
    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret); // 시크릿 ON이면 반드시 포함
        }

        ResponseEntity<JsonNode> res = restTemplate.postForEntity(
                tokenUri, new HttpEntity<>(form, headers), JsonNode.class);

        JsonNode body = res.getBody();
        if (body == null || !body.hasNonNull("access_token")) {
            log.error("[KAKAO] 토큰 교환 실패: {}", body);
            throw new IllegalStateException("카카오 토큰 교환에 실패했습니다.");
        }
        return body.get("access_token").asText();
    }

    private SocialProfile requestProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> res = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        JsonNode root = res.getBody();
        if (root == null || !root.hasNonNull("id")) {
            log.error("[KAKAO] 프로필 조회 실패: {}", root);
            throw new IllegalStateException("카카오 프로필 조회에 실패했습니다.");
        }

        String providerUserId = root.get("id").asText();       // 카카오 회원번호(숫자)
        JsonNode account = root.path("kakao_account");

        String email = emptyToNull(account.path("email").asText(null));
        // 이름은 비즈앱에서만 옴 → 없으면 닉네임으로 대체
        String name = emptyToNull(account.path("name").asText(null));
        if (name == null) {
            name = emptyToNull(account.path("profile").path("nickname").asText(null));
        }
        String phone = normalizePhone(account.path("phone_number").asText(null));
        LocalDate birthDate = parseBirth(account.path("birthyear").asText(null), account.path("birthday").asText(null));
        Gender gender = parseGender(account.path("gender").asText(null));

        return new SocialProfile(providerUserId, email, name, phone, birthDate, gender);
    }

    // 카카오 gender: "male"/"female"
    private Gender parseGender(String g) {
        if ("male".equalsIgnoreCase(g)) return Gender.MALE;
        if ("female".equalsIgnoreCase(g)) return Gender.FEMALE;
        return Gender.NONE;
    }

    // birthyear "1990" + birthday "0327"(MMDD) → 1990-03-27
    private LocalDate parseBirth(String birthyear, String birthday) {
        if (!StringUtils.hasText(birthyear) || !StringUtils.hasText(birthday) || birthday.length() != 4) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(birthyear.trim()),
                    Integer.parseInt(birthday.substring(0, 2)), Integer.parseInt(birthday.substring(2, 4)));
        } catch (RuntimeException e) {
            log.warn("[KAKAO] 생년월일 파싱 실패 year={} day={}", birthyear, birthday);
            return null;
        }
    }

    // 카카오 phone_number: "+82 10-1234-5678" → "01012345678"
    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}