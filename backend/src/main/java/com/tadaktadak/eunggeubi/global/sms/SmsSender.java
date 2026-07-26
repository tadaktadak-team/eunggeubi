package com.tadaktadak.eunggeubi.global.sms;

public interface SmsSender {
    // 나중에 Solapi 구현체(SolapiSmsSender)로 갈아끼우면 됨
    void send(String to, String text);
}