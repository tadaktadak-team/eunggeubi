package com.tadaktadak.eunggeubi.global.sms;

public interface SmsSender {
    // 나중에 Solapi 구현체(SolapiSmsSender)로 갈아끼우면 됨
    void send(String to, String text);

    // 제목이 필요한 장문(LMS)용. 구현이 없으면 기존 방식으로 보낸다.
    default void send(String to, String subject, String text) {
        send(to, text);
    }
}
