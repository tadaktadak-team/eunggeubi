package com.tadaktadak.eunggeubi.global.sms;

import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SolapiSmsSender implements SmsSender {

    private final DefaultMessageService messageService;
    private final String from;

    public SolapiSmsSender(
            @Value("${solapi.api-key}") String apiKey,
            @Value("${solapi.api-secret}") String apiSecret,
            @Value("${solapi.from}") String from) {
        this.messageService = NurigoApp.INSTANCE.initialize(
                apiKey, apiSecret, "https://api.solapi.com");
        this.from = from;
    }

    @Override
    public void send(String to, String text) {
        send(to, null, text);
    }

    @Override
    public void send(String to, String subject, String text) {
        Message message = new Message();
        message.setFrom(from);
        message.setTo(to);
        message.setText(text);
        if (subject != null) {
            message.setSubject(subject);
        }

        try {
            SingleMessageSentResponse response =
                    messageService.sendOne(new SingleMessageSendingRequest(message));
            log.info("[SOLAPI] 발송 완료 to={} statusCode={}", to,
                    response != null ? response.getStatusCode() : "null");
        } catch (Exception e) {
            log.error("[SOLAPI] 발송 실패 to={}", to, e);
            throw new IllegalStateException("문자 발송에 실패했습니다.", e);
        }
    }
}