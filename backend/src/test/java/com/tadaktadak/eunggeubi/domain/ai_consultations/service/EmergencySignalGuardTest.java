package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmergencySignalGuardTest {

    @Test
    void 응급_징후_문장이면_true() {
        assertThat(EmergencySignalGuard.containsEmergencySignal("의식을 잃었어요")).isTrue();
        assertThat(EmergencySignalGuard.containsEmergencySignal("불러도 반응이 없어요")).isTrue();
        assertThat(EmergencySignalGuard.containsEmergencySignal("피가 멎지 않아요")).isTrue();
        assertThat(EmergencySignalGuard.containsEmergencySignal("피가 안 멎어요")).isTrue(); // 구어체
        assertThat(EmergencySignalGuard.containsEmergencySignal("출혈이 안 멈춰요")).isTrue(); // 구어체
        assertThat(EmergencySignalGuard.containsEmergencySignal("과다출혈이 있어요")).isTrue();
        assertThat(EmergencySignalGuard.containsEmergencySignal("숨을 못 쉬겠어요")).isTrue();
        assertThat(EmergencySignalGuard.containsEmergencySignal("숨을 쉴 수가 없어요")).isTrue(); // "가" 조사
        assertThat(EmergencySignalGuard.containsEmergencySignal("호흡곤란이 있어요")).isTrue();
    }

    @Test
    void 흔한_증상_문장이면_false() {
        assertThat(EmergencySignalGuard.containsEmergencySignal("두통이 있어요")).isFalse();
        assertThat(EmergencySignalGuard.containsEmergencySignal("배가 아파요")).isFalse();
        assertThat(EmergencySignalGuard.containsEmergencySignal("어지러워요")).isFalse();
    }

    @Test
    void null_또는_빈_문자열이면_false() {
        assertThat(EmergencySignalGuard.containsEmergencySignal(null)).isFalse();
        assertThat(EmergencySignalGuard.containsEmergencySignal("")).isFalse();
        assertThat(EmergencySignalGuard.containsEmergencySignal("   ")).isFalse();
    }
}
