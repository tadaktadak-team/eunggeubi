package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

// 비회원 무료 AI 상담 소진 -> 403. 질문 수 한도(GUEST_MESSAGE_LIMIT)와 IP당 guestCode 발급 한도
// (GUEST_CODES_PER_IP_PER_DAY) 둘 다 이걸 던지므로, 문구에 "10회" 같은 특정 숫자를 넣지 않는다 -
// 공유 와이파이에서 처음 쓰는 사람이 발급 한도에 걸려도 "10회를 다 썼다"고 안내하면 틀린 말이 된다.
// 프론트는 응답의 code(GUEST_LIMIT)로 회원가입 안내 시트를 띄운다.
public class GuestLimitExceededException extends RuntimeException {
    public GuestLimitExceededException() {
        super("비회원 무료 AI 상담을 모두 사용했어요. 회원가입 후 계속 이용해주세요.");
    }
}
