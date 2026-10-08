package com.tadaktadak.eunggeubi.domain.auth.dto;

// 보호자 동의 안내 화면(GET /api/auth/guardian/confirm)에 뿌릴 정보.
// 이 단계에서는 상태를 바꾸지 않는다 - 실제 확정은 보호자가 버튼을 눌러 POST 했을 때만 한다.
public record ConsentPageInfo(State state, String childName) {

    public enum State {
        READY,              // 동의 가능 - 폼을 보여준다
        ALREADY_CONFIRMED,  // 이미 동의 완료
        EXPIRED,            // 유효기간(3일) 지남
        INVALID             // 토큰이 없거나 잘못됨
    }
}