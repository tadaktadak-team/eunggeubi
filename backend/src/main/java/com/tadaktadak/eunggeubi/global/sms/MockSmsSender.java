package com.tadaktadak.eunggeubi.global.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String to, String text) {
        // 실제 발송 대신 로그만 (개발용). 콘솔에서 인증번호 확인 가능
        log.info("[MOCK SMS] 수신자={} | 내용={}", to, text);
    }
}