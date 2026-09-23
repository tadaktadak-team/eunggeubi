package com.tadaktadak.eunggeubi.domain.emergency.service;

import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyAlertNotifier {

    private final SmsSender smsSender;

    @Async("smsExecutor")
    public void notifyGuardian(String phone, String subject, String message) {
        try {
            smsSender.send(phone, subject, message);
        } catch (Exception e) {
            log.error("[EMERGENCY] 보호자 문자 발송 실패 phone={}", phone, e);
        }
    }
}