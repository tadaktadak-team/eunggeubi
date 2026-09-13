package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import java.util.regex.Pattern;

// 사용자 입력에 명백한 응급 징후가 있는지 코드로 직접 검사한다. AiConsultationPrompts.CONSULT_PROMPT가
// 모델에게 "이 세 가지 징후가 있으면 119 안내를 넣어라"라고 지시하지만, 모델이 그 규칙을 놓칠 수 있어서
// (진짜 응급인데 119 안내가 안 나가면 피해로 이어짐) 모델 응답과 무관하게 동작하는 마지막 방어선을 둔다.
// DiagnosisLanguageGuard와 같은 이유로 완벽한 자연어 이해가 아니라 키워드 휴리스틱이다.
public final class EmergencySignalGuard {

    private static final Pattern EMERGENCY_PATTERN = Pattern.compile(
            "의식을?\\s*잃|정신을?\\s*잃|의식이?\\s*없|반응이?\\s*없" // 의식 소실
                    // "피가 안 멎어요"(구어체)와 "피가 멎지 않아요"(문어체)를 각각 잡는다. 예전 정규식은
                    // "(안)? ... 멎지 않"처럼 둘을 잘못 합쳐놔서, "안 멎어요"처럼 훨씬 흔한 표현을 놓쳤다.
                    // "멈추다"는 "멈춰요"로 불규칙 활용해서(어간 "멈추"가 그대로 안 남음) "멈추"만 찾으면 놓친다.
                    + "|(피|출혈)(가|이)?\\s*(안\\s*(멎|멈추|멈춰)|(멎|멈추)지\\s*않)|과다\\s*출혈|출혈이?\\s*심" // 심한 출혈
                    // "쉴 수(가) 없어요"의 "가"처럼 조사가 끼어드는 경우를 놓치지 않게 (가|는)?를 허용한다.
                    + "|숨을?\\s*못\\s*쉬|숨이?\\s*안\\s*쉬|호흡\\s*곤란|숨쉬기가?\\s*힘들|숨을?\\s*쉴\\s*수(가|는)?\\s*없" // 호흡곤란
    );

    private EmergencySignalGuard() {
    }

    public static boolean containsEmergencySignal(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return EMERGENCY_PATTERN.matcher(text).find();
    }
}
