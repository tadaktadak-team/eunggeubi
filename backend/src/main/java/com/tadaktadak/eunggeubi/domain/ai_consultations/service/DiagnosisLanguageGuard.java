package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.regex.Pattern;

// 재생성 응답에 "진단을 확정하는" 표현이 섞였는지 정규식으로 검사한다. 프롬프트로 진단하지 말라고
// 지시해도(AiConsultationPrompts.REGENERATE_PROMPT) 모델이 이걸 어길 수 있어서 마지막 방어선으로 둔다.
// 완벽한 자연어 이해가 아니라 흔한 진단 확정 어미 패턴을 잡아내는 휴리스틱이다 - 걸리면 통째로 안전한
// 문구로 교체한다(부분 수정은 문장을 어색하게 만들 위험이 커서 하지 않는다).
public final class DiagnosisLanguageGuard {

    private static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile(
            // "이 증상은 OOO입니다" 형태 - 사용자가 말한 증상을 가리키는 지시어(이/해당/귀하의) 뒤에
            // "증상은 ~이다"가 오면 진단 확정 문장으로 본다. 지시어를 빼면 "주요 증상은 발열입니다",
            // "동반 증상은 오한입니다"처럼 참고자료가 그 병의 일반적인 증상을 나열하는 정상 문장까지
            // 걸려서(실측 확인) 답변 전체가 불필요하게 폴백 문구로 교체되는 오탐이 잦았다.
            "(이|해당|귀하의)\\s*증상은\\s*[^.!?\\n]{0,30}(입니다|이에요|이다|예요)"
                    + "|로\\s*진단(됩니다|합니다|되었습니다|됨)"
                    // 접미사를 옵션(?)으로 두면 "정확한 확진을 위해서는..." 같은 안내성 문장의 "확진"
                    // 단어만으로도 매칭됐다(실측 확인) - 반드시 확정 어미가 붙어야만 잡히게 한다.
                    + "|확진(입니다|되었습니다|됩니다)"
                    + "|이\\s*확실합니다"
                    + "|임이\\s*틀림없습니다"
                    + "|에\\s*걸리셨습니다"
                    + "|을\\s*앓고\\s*계십니다"
                    // "이십니다"는 상대를 직접 존대해 지칭하는 어미라 "환자의 10%는..." 같은 유병률
                    // 통계 문장과 구분된다 - 존대 없는 "환자입니다/환자예요"는 그런 통계성 서술과
                    // 안 구분돼서 일부러 넣지 않는다.
                    + "|환자이십니다"
                    + "|진단(을)?\\s*받(으)?[셨았]습니다"
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
