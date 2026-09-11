package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

// "의료 자문을 대체하지 않습니다" 고지 문구. AiConsultationService(1차 답변)/ConsultationRegenerationService
// (재생성)/ConsultationHistoryService(이력 조회)가 다 같은 문구를 쓰도록 한곳에 모아둔다 - 예전엔 세 군데가
// 각자 다른 문구를 갖고 있었다. 모델에게 매번 붙이라고 프롬프트로 부탁하는 대신 서버가 응답에서 결정적으로
// 붙인다(빠뜨리는 경우가 실제로 있었다). DB에는 저장하지 않고 응답에서만 붙인다.
public final class ConsultationDisclaimer {

    public static final String TEXT = "이 정보는 참고용이며 진단이 아닙니다. 증상이 지속되면 의료진과 상담하세요.";

    private ConsultationDisclaimer() {
    }
}
