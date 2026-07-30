package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Gender;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import java.time.LocalDate;

public record ProfileResponse(
        Long userId,
        String email,       // 조회만, 수정 불가
        String name,
        String phone,
        LocalDate birthDate,
        Gender gender,
        String address,
        UserStatus status
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getBirthDate(),
                user.getGender(),
                user.getAddress(),
                user.getStatus()
        );
    }
}