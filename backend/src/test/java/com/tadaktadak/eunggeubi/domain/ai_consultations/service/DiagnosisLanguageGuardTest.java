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
    }

    @Test
    void 일반_안내_문장이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("충분한 휴식과 수분 섭취가 도움이 됩니다."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("증상이 3일 이상 지속되면 병원 진료를 받아보세요."))
                .isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("이 안내는 의료 자문을 대체하지 않습니다."))
                .isFalse();
    }

    @Test
    void null_또는_빈_문자열이면_false() {
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage(null)).isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("")).isFalse();
        assertThat(DiagnosisLanguageGuard.containsDiagnosticLanguage("   ")).isFalse();
    }
}
