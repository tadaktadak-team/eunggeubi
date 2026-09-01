package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.regex.Pattern;

// 재생성 응답에 "진단을 확정하는" 표현이 섞였는지 정규식으로 검사한다. 프롬프트로 진단하지 말라고
// 지시해도(AiConsultationPrompts.REGENERATE_PROMPT) 모델이 이걸 어길 수 있어서 마지막 방어선으로 둔다.
// 완벽한 자연어 이해가 아니라 흔한 진단 확정 어미 패턴을 잡아내는 휴리스틱이다 - 걸리면 통째로 안전한
// 문구로 교체한다(부분 수정은 문장을 어색하게 만들 위험이 커서 하지 않는다).
public final class DiagnosisLanguageGuard {

    private static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile(
            "(병|질환|질병|증후군|염)\\s*(입니다|이에요|이다|예요)"
                    // "이 증상은 OOO입니다" 형태 - 질병명이 무엇이든 "증상은 ~이다" 서술 구조 자체가
                    // 진단 확정 문장이라, 특정 질병명 접미사에 기대지 않고 이 문장 구조로 잡는다.
                    + "|증상은\\s*[^.!?\\n]{0,30}(입니다|이에요|이다|예요)"
                    + "|로\\s*진단(됩니다|합니다|되었습니다|됨)"
                    + "|확진(입니다|되었습니다|됩니다)?"
                    + "|이\\s*확실합니다"
                    + "|임이\\s*틀림없습니다"
                    + "|에\\s*걸리셨습니다"
                    + "|을\\s*앓고\\s*계십니다"
    );

    private DiagnosisLanguageGuard() {
    }

    public static boolean containsDiagnosticLanguage(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return DIAGNOSTIC_PATTERN.matcher(text).find();
    }
}
