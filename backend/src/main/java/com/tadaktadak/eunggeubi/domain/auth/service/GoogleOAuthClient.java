package com.tadaktadak.eunggeubi.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialProfile;
import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
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
 * 구글 로그인 OAuth 처리기.
 * authorize(scope 포함) → 인가코드로 토큰 교환(POST) → 토큰으로 프로필 조회 → SocialProfile 로 정규화.
 * 구글은 전화/생년월일/성별을 기본 제공하지 않아 그 값은 null(→ 추가정보 화면에서 받음).
 */
@Slf4j
@Component
public class GoogleOAuthClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String authorizeUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String scope;

    public GoogleOAuthClient(
            RestTemplate restTemplate,
            @Value("${oauth.google.client-id:}") String clientId,
            @Value("${oauth.google.client-secret:}") String clientSecret,
            @Value("${oauth.google.redirect-uri:}") String redirectUri,
            @Value("${oauth.google.authorize-uri:}") String authorizeUri,
            @Value("${oauth.google.token-uri:}") String tokenUri,
            @Value("${oauth.google.userinfo-uri:}") String userInfoUri,
            @Value("${oauth.google.scope:openid email profile}") String scope) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizeUri = authorizeUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.scope = scope;
    }

    // 1) 구글 로그인 페이지 URL (scope 필수)
    public String buildAuthorizeUrl(String state) {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException("구글 client-id 가 설정되지 않았습니다. (oauth.google.client-id)");
        }
        return UriComponentsBuilder.fromUriString(authorizeUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
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

    // 구글 토큰 교환: POST + x-www-form-urlencoded (client_secret 필수)
    private String requestAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        ResponseEntity<JsonNode> res = restTemplate.postForEntity(
                tokenUri, new HttpEntity<>(form, headers), JsonNode.class);

        JsonNode body = res.getBody();
        if (body == null || !body.hasNonNull("access_token")) {
            log.error("[GOOGLE] 토큰 교환 실패: {}", body);
            throw new IllegalStateException("구글 토큰 교환에 실패했습니다.");
        }
        return body.get("access_token").asText();
    }

    private SocialProfile requestProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<JsonNode> res = restTemplate.exchange(
                userInfoUri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        JsonNode root = res.getBody();
        if (root == null || !root.hasNonNull("sub")) {
            log.error("[GOOGLE] 프로필 조회 실패: {}", root);
            throw new IllegalStateException("구글 프로필 조회에 실패했습니다.");
        }

        String providerUserId = root.get("sub").asText();          // 구글 고유 id
        String email = emptyToNull(root.path("email").asText(null));
        String name = emptyToNull(root.path("name").asText(null));
        // 전화/생년월일/성별은 구글 기본 제공 X → null (추가정보 화면에서 입력)
        return new SocialProfile(providerUserId, email, name, null, null, Gender.NONE);
    }

    private String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}