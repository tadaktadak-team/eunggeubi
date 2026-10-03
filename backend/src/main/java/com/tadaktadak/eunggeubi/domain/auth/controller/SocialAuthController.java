package com.tadaktadak.eunggeubi.domain.auth.controller;
import com.tadaktadak.eunggeubi.domain.auth.dto.LoginResponse;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialLoginExchangeRequest;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialProfile;
import com.tadaktadak.eunggeubi.domain.auth.dto.SocialSignupCompleteRequest;
import com.tadaktadak.eunggeubi.domain.auth.entity.Provider;
import com.tadaktadak.eunggeubi.domain.auth.service.AppRedirectValidator;
import com.tadaktadak.eunggeubi.domain.auth.service.KakaoOAuthClient;
import com.tadaktadak.eunggeubi.domain.auth.service.GoogleOAuthClient;
import com.tadaktadak.eunggeubi.domain.auth.service.NaverOAuthClient;
import com.tadaktadak.eunggeubi.domain.auth.service.SocialAuthService;
import com.tadaktadak.eunggeubi.domain.auth.service.SocialAuthStateStore;
import com.tadaktadak.eunggeubi.domain.auth.service.SocialLoginTicketStore;
import com.tadaktadak.eunggeubi.global.exception.SocialEmailConflictException;
import com.tadaktadak.eunggeubi.domain.auth.service.SocialSignupTicketStore;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final NaverOAuthClient naverOAuthClient;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final SocialAuthService socialAuthService;
    private final SocialAuthStateStore stateStore;
    private final SocialSignupTicketStore ticketStore;
    private final AppRedirectValidator appRedirectValidator;
    private final SocialLoginTicketStore loginTicketStore;

    // ===== 네이버 =====
    @GetMapping("/naver/authorize")
    public ResponseEntity<Void> naverAuthorize(@RequestParam String appRedirect) {
        String state = newState(appRedirect);
        return redirect(naverOAuthClient.buildAuthorizeUrl(state));
    }

    @GetMapping("/naver/callback")
    public ResponseEntity<Void> naverCallback(@RequestParam String code, @RequestParam String state) {
        String appRedirect = requireAppRedirect(state);
        SocialProfile profile = naverOAuthClient.fetchProfile(code, state);
        return redirectToApp(Provider.NAVER, appRedirect, profile);
    }

    // ===== 카카오 =====
    @GetMapping("/kakao/authorize")
    public ResponseEntity<Void> kakaoAuthorize(@RequestParam String appRedirect) {
        String state = newState(appRedirect);
        return redirect(kakaoOAuthClient.buildAuthorizeUrl(state));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(@RequestParam String code, @RequestParam String state) {
        String appRedirect = requireAppRedirect(state);
        SocialProfile profile = kakaoOAuthClient.fetchProfile(code);
        return redirectToApp(Provider.KAKAO, appRedirect, profile);
    }

    // ===== 구글 =====
    @GetMapping("/google/authorize")
    public ResponseEntity<Void> googleAuthorize(@RequestParam String appRedirect) {
        String state = newState(appRedirect);
        return redirect(googleOAuthClient.buildAuthorizeUrl(state));
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(@RequestParam String code, @RequestParam String state) {
        String appRedirect = requireAppRedirect(state);
        SocialProfile profile = googleOAuthClient.fetchProfile(code);
        return redirectToApp(Provider.GOOGLE, appRedirect, profile);
    }


    // ===== 공통 =====
    @PostMapping("/complete")
    public ResponseEntity<LoginResponse> complete(@Valid @RequestBody SocialSignupCompleteRequest request) {
        SocialSignupTicketStore.Pending pending = ticketStore.consume(request.ticket());
        if (pending == null) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 가입 요청입니다. 다시 시도해주세요.");
        }
        LoginResponse result = socialAuthService.completeSignup(
                pending.provider(), pending.profile(),
                request.phone(), request.birthDate(), request.gender());
        return ResponseEntity.ok(result);
    }
    // 콜백에서 받은 loginTicket 을 실제 토큰으로 교환한다(1회용, 60초).
    // 토큰을 리다이렉트 URL 에 싣지 않기 위한 단계다.
    @PostMapping("/exchange")
    public ResponseEntity<LoginResponse> exchange(@Valid @RequestBody SocialLoginExchangeRequest request) {
        LoginResponse tokens = loginTicketStore.consume(request.ticket());
        if (tokens == null) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 로그인 요청입니다. 다시 시도해주세요.");
        }
        return ResponseEntity.ok(tokens);
    }

    // state 생성 + 저장. 저장 전에 appRedirect 가 우리 앱 주소인지 반드시 검증한다
    // (검증 없이 저장하면 콜백에서 그 주소로 토큰을 그대로 보내게 된다).
    private String newState(String appRedirect) {
        appRedirectValidator.validate(appRedirect);
        String state = UUID.randomUUID().toString().replace("-", "");
        stateStore.save(state, appRedirect);
        return state;
    }

    // state 검증 후 저장해둔 앱 복귀주소 반환
    private String requireAppRedirect(String state) {
        String appRedirect = stateStore.consume(state);
        if (appRedirect == null) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 로그인 요청입니다.");
        }
        return appRedirect;
    }

    // 로그인 페이지로 302
    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    // 콜백 공통: 기존 회원이든 신규든 1회용 ticket 만 앱으로 넘긴다(토큰은 URL 에 싣지 않는다).
    private ResponseEntity<Void> redirectToApp(Provider provider, String appRedirect, SocialProfile profile) {
        Optional<LoginResponse> existing;
        try {
            existing = socialAuthService.loginIfExisting(provider, profile);
        } catch (SocialEmailConflictException e) {
            // 여기는 인앱 브라우저가 열고 있는 콜백 화면이라, 예외를 던지면 사용자가 날 JSON 을 보게 된다.
            // 앱으로 되돌려 보내고 앱이 안내 문구를 띄우게 한다.
            return redirect(UriComponentsBuilder.fromUriString(appRedirect)
                    .queryParam("error", "email_taken")
                    .encode().build().toUriString());
        }
        String redirect;
        if (existing.isPresent()) {
            // eunggeubi:// 는 소유권 검증이 없는 커스텀 스킴이라 다른 앱이 가로챌 수 있고,
            // URL 은 OS 로그에도 남는다. 그래서 토큰 대신 60초짜리 1회용 티켓만 넘기고
            // 앱이 POST /api/auth/social/exchange 로 본문에서 토큰을 받아간다.
            String loginTicket = UUID.randomUUID().toString().replace("-", "");
            loginTicketStore.save(loginTicket, existing.get());
            redirect = UriComponentsBuilder.fromUriString(appRedirect)
                    .queryParam("loginTicket", loginTicket)
                    .encode().build().toUriString();
        } else {
            String ticket = UUID.randomUUID().toString().replace("-", "");
            ticketStore.save(ticket, provider, profile);
            // 제공자가 전화/생년월일을 안 줬으면 앱에서 추가정보 입력이 필요하다(카카오 등)
            boolean needInfo = profile.phone() == null || profile.birthDate() == null;
            redirect = UriComponentsBuilder.fromUriString(appRedirect)
                    .queryParam("needConsent", true)
                    .queryParam("needInfo", needInfo)
                    .queryParam("ticket", ticket)

                    .encode().build().toUriString();
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirect)).build();
    }
}