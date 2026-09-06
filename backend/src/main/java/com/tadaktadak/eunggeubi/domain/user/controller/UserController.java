package com.tadaktadak.eunggeubi.domain.user.controller;

import com.tadaktadak.eunggeubi.domain.user.dto.ChangePasswordRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.ChangePasswordResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.MyInfoResponse;
import com.tadaktadak.eunggeubi.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //마이페이지 예약 조회
    @GetMapping("/me")
    public MyInfoResponse getMyInfo(@AuthenticationPrincipal Long userId) {
        return userService.getMyInfo(userId);
    }

    //비밀번호 변경
    @PutMapping("/password")
    public ChangePasswordResponse changePassword(@AuthenticationPrincipal Long userId,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
}