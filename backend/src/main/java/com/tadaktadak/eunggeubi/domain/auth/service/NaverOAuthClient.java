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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 네이버 로그인 OAuth 처리기.
 * authorize URL 생성 → 인가코드로 토큰 교환 → 토큰으로 프로필 조회 → SocialProfile 로 정규화.
 * 응답 스펙: https://developers.naver.com/docs/login/profile/profile.md
 */
@Slf4j
@Component
public class NaverOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizeUri;
    private final String tokenUri;
    private final String userInfoUri;

    public NaverOAuthClient(
            RestTemplate restTemplate,
            @Value("${oauth.naver.client-id:}") String clientId,
            @Value("${oauth.naver.client-secret:}") String clientSecret,
            @Value("${oauth.naver.redirect-uri:}") String redirectUri,
            @Value("${oauth.naver.authorize-uri:}") String authorizeUri,
            @Value("${oauth.naver.token-uri:}") String tokenUri,
            @Value("${oauth.naver.userinfo-uri:}") String userInfoUri) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizeUri = authorizeUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    // 1) 사용자를 로그인시킬 네이버 인가 페이지 URL 생성
    public String buildAuthorizeUrl(String state) {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException("네이버 client-id 가 설정되지 않았습니다. (oauth.naver.client-id)");
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
    public SocialProfile fetchProfile(String code, String state) {
        String accessToken = requestAccessToken(code, state);
        return requestProfile(accessToken);
    }

    private String requestAccessToken(String code, String state) {
        String url = UriComponentsBuilder.fromUriString(tokenUri)
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("code", code)
                .queryParam("state", state)
                .encode()
                .build()
                .toUriString();

        JsonNode body = restTemplate.getForObject(url, JsonNode.class);
        if (body == null || !body.hasNonNull("access_token")) {
            log.error("[NAVER] 토큰 교환 실패: {}", body);
            throw new IllegalStateException("네이버 토큰 교환에 실패했습니다.");
        }
        return body.get("access_token").asText();
    }

    private SocialProfile requestProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> res = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        JsonNode root = res.getBody();
        if (root == null || !"00".equals(root.path("resultcode").asText())) {
            log.error("[NAVER] 프로필 조회 실패: {}", root);
            throw new IllegalStateException("네이버 프로필 조회에 실패했습니다.");
        }
        JsonNode r = root.path("response");

        String providerUserId = r.path("id").asText(null);
        String email = emptyToNull(r.path("email").asText(null));
        String name = emptyToNull(r.path("name").asText(null));
        String phone = normalizePhone(r.path("mobile").asText(null));
        LocalDate birthDate = parseBirth(r.path("birthyear").asText(null), r.path("birthday").asText(null));
        Gender gender = parseGender(r.path("gender").asText(null));

        return new SocialProfile(providerUserId, email, name, phone, birthDate, gender);
    }

    // 네이버 gender: "M"/"F"/"U" → 우리 Gender
    private Gender parseGender(String g) {
        if ("M".equalsIgnoreCase(g)) return Gender.MALE;
        if ("F".equalsIgnoreCase(g)) return Gender.FEMALE;
        return Gender.NONE;
    }

    // birthyear "1990" + birthday "03-27" → 1990-03-27
    private LocalDate parseBirth(String birthyear, String birthday) {
        if (!StringUtils.hasText(birthyear) || !StringUtils.hasText(birthday) || !birthday.contains("-")) {
            return null;
        }
        try {
            String[] md = birthday.split("-");
            return LocalDate.of(Integer.parseInt(birthyear.trim()),
                    Integer.parseInt(md[0].trim()), Integer.parseInt(md[1].trim()));
        } catch (RuntimeException e) {
            log.warn("[NAVER] 생년월일 파싱 실패 year={} day={}", birthyear, birthday);
            return null;
        }
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return null;
        return phone.replaceAll("[^0-9]", "");
    }

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}