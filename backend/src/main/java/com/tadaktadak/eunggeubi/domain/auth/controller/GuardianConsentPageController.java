package com.tadaktadak.eunggeubi.domain.auth.controller;

import com.tadaktadak.eunggeubi.domain.auth.dto.ConsentPageInfo;
import com.tadaktadak.eunggeubi.domain.auth.service.GuardianConsentService;
import com.tadaktadak.eunggeubi.domain.user.entity.ConsentPurpose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 보호자가 문자 링크로 들어오는 웹 화면. 앱이 아니라 모바일 브라우저에서 열린다.
 *
 * GET 은 안내 화면만 보여주고 아무것도 바꾸지 않는다. GET 으로 확정하면 통신사 스팸필터·
 * 메신저 링크 미리보기·보안 스캐너가 링크를 열기만 해도 본인 의사와 무관하게 동의 처리된다.
 * 법정대리인 동의는 "본인이 눌렀다"는 사실이 핵심이라 POST 로 분리한다.
 *
 * JSON 이 아니라 화면을 반환하므로 @RestController 가 아니라 @Controller 다(뷰 이름 반환).
 */
@Controller
@RequiredArgsConstructor
public class GuardianConsentPageController {

    private static final String VIEW = "guardian-consent";

    private final GuardianConsentService guardianConsentService;

    @GetMapping("/consent")
    public String page(@RequestParam("t") String token, Model model) {
        ConsentPageInfo info = guardianConsentService.loadConsentPage(token);
        boolean ready = info.state() == ConsentPageInfo.State.READY;
        model.addAttribute("ready", ready);

        if (ready) {
            String child = (info.childName() == null || info.childName().isBlank())
                    ? "자녀" : info.childName();
            model.addAttribute("childName", child);
            model.addAttribute("token", token);
            model.addAttribute("registration", info.purpose() == ConsentPurpose.REGISTRATION);
            return VIEW;
        }

        switch (info.state()) {
            case ALREADY_CONFIRMED -> fill(model, "이미 동의가 완료되었습니다.",
                    info.purpose() == ConsentPurpose.REGISTRATION
                            ? "긴급 상황이 생기면 알림 문자를 받게 돼요."
                            : "자녀가 앱에서 로그인할 수 있어요.");
            case EXPIRED -> fill(model, "만료된 링크입니다.", "앱에서 동의 문자를 다시 요청해주세요.");
            default -> fill(model, "유효하지 않은 링크입니다.", "문자에 포함된 주소가 맞는지 확인해주세요.");
        }
        return VIEW;
    }

    // 보호자가 "동의합니다" 버튼을 눌렀을 때만 실제로 확정한다.
    @PostMapping("/consent")
    public String confirm(@RequestParam("t") String token, Model model) {
        model.addAttribute("ready", false);
        try {
            ConsentPurpose purpose = guardianConsentService.confirmConsent(token);
            if (purpose == ConsentPurpose.REGISTRATION) {
                fill(model, "보호자 등록에 동의하셨습니다.", "이제 긴급 상황이 생기면 알림 문자를 받게 돼요.");
            } else {
                fill(model, "동의가 완료되었습니다.", "이제 자녀가 앱에서 로그인할 수 있어요.");
            }
        } catch (IllegalArgumentException e) {
            fill(model, "동의를 완료하지 못했습니다.", e.getMessage());
        }
        return VIEW;
    }

    private void fill(Model model, String title, String detail) {
        model.addAttribute("title", title);
        model.addAttribute("detail", detail);
    }
}