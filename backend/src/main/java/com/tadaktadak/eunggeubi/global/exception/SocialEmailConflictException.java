package com.tadaktadak.eunggeubi.global.exception;

/**
 * 소셜 로그인으로 들어온 이메일이 이미 가입된 계정의 이메일일 때 던진다.
 *
 * 이메일이 같다는 이유만으로 기존 계정에 연동하면, 제공자가 이메일 소유를 보장하지 않는 이상
 * (카카오는 선택 동의 + 미인증 이메일도 내려온다) 남의 계정을 그대로 가져갈 수 있다.
 *
 * 콜백(브라우저 리다이렉트)에서는 컨트롤러가 이걸 잡아 앱으로 되돌려 보내고,
 * /complete(앱이 직접 호출)에서는 409 로 응답한다.
 */
public class SocialEmailConflictException extends RuntimeException {

    public SocialEmailConflictException() {
        super("이미 가입된 이메일입니다. 기존에 사용하던 방법으로 로그인해주세요.");
    }
}