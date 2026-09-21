package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiagnosisLanguageGuardTest {

    @Test
    void 진단_확정형_표현이면_true() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("이 증상은 편두통입니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("맹장염으로 진단됩니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("독감이 확진되었습니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("장염임이 틀림없습니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("식중독에 걸리셨습니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("당뇨병 환자이십니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("편두통 진단을 받으셨습니다.")).isTrue();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("장염 진단받았습니다.")).isTrue();
    }

    @Test
    void 일반_안내_문장이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("충분한 휴식과 수분 섭취가 도움이 됩니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("증상이 3일 이상 지속되면 병원 진료를 받아보세요."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("이 안내는 의료 자문을 대체하지 않습니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("편두통이 의심됩니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("장염으로 보입니다."))
                .isFalse();
    }

    // "확진(입니다|되었습니다|됩니다)?"처럼 확정 어미를 옵션으로 두면 "정확한 확진을 위해서는..."
    // 같은 안내성 문장의 "확진" 단어만으로도 매칭되는 오탐이 있었다(실측 확인) - 확정 어미가
    // 반드시 붙어야만 진단 확정으로 본다.
    @Test
    void 확진이라는_단어만_있고_안내성_문장이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("정확한 확진을 위해서는 병원에서 검사가 필요합니다."))
                .isFalse();
    }

    // 참고자료를 인용/요약하다 보면 그 병의 일반적인 증상을 나열하는 문장이 자연스럽게 나오는데,
    // 사용자 증상을 가리키는 지시어(이/해당/귀하의)가 없으면 진단이 아니라 정보성 설명이다 -
    // 예전 정규식은 이런 문장까지 걸려서 답변 전체가 불필요하게 폴백 문구로 교체되는 오탐이 있었다.
    @Test
    void 병에_대한_일반_정보_설명이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("감기는 흔한 호흡기 질환입니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("동반 증상은 오한입니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("주요 증상은 발열입니다."))
                .isFalse();
    }

    @Test
    void null_또는_빈_문자열이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage(null)).isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("")).isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("   ")).isFalse();
    }
}
