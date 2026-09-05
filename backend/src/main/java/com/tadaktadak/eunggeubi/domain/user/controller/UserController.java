package com.tadaktadak.eunggeubi.domain.user.controller;

import com.tadaktadak.eunggeubi.domain.user.dto.ChangePasswordRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.MyInfoResponse;
import com.tadaktadak.eunggeubi.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // 마이페이지 요약 조회 (MY01_INFO01)
    @GetMapping("/me")
    public MyInfoResponse getMyInfo(@AuthenticationPrincipal Long userId) {
        return userService.getMyInfo(userId);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}