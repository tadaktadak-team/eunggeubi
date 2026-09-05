package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import java.time.LocalDate;

// 마이페이지 요약 조회(MY01_INFO01). 회원 정보 수정(MEM03) 화면의 초기값으로도 그대로 쓸 수 있게
// 수정 가능한 필드를 함께 내려준다.
// password/status 같은 내부 값은 노출하지 않는다.
public record MyInfoResponse(
        String email,
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String address,
        // 소셜로만 가입해 비밀번호가 없는 계정. 프론트가 "비밀번호 변경" 메뉴를 미리 숨기는 데 쓴다
        // (누르고 입력까지 한 뒤에 거절당하는 것보다 낫다).
        boolean socialOnly
) {
    public static MyInfoResponse from(User user) {
        return new MyInfoResponse(
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getGender(),
                user.getAddress(),
                user.getPassword() == null
        );
    }
}
