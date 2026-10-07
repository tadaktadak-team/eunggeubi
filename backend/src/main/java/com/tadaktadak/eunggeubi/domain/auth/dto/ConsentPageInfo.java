package com.tadaktadak.eunggeubi.domain.auth.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.ConsentPurpose;

// 보호자 동의 안내 화면(GET /consent)에 뿌릴 정보.
// 이 단계에서는 상태를 바꾸지 않는다 - 실제 확정은 보호자가 버튼을 눌러 POST 했을 때만 한다.
// purpose: 미성년자 가입 동의(SIGNUP)인지, 이미 가입한 회원이 보호자로 등록한 동의(REGISTRATION)인지에 따라 문구가 다르다.
public record ConsentPageInfo(State state, String childName, ConsentPurpose purpose) {

    public enum State {
        READY,              // 동의 가능 - 폼을 보여준다
        ALREADY_CONFIRMED,  // 이미 동의 완료
        EXPIRED,            // 유효기간(3일) 지남
        INVALID             // 토큰이 없거나 잘못됨
    }
}