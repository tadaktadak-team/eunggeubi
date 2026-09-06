package com.tadaktadak.eunggeubi.domain.user.controller;

import com.tadaktadak.eunggeubi.domain.user.dto.ChangePasswordRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.MyInfoResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.UpdateProfileRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.WithdrawRequest;
import com.tadaktadak.eunggeubi.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 마이페이지 요약 조회(MY01_INFO01) 겸 회원 정보 수정(MEM03) 화면의 초기값.
    // 두 화면이 필요로 하는 필드가 같아서 응답을 MyInfoResponse 하나로 통일했다.
    @GetMapping("/me")
    public MyInfoResponse getMyInfo(@AuthenticationPrincipal Long userId) {
        return userService.getMyInfo(userId);
    }

    @PostMapping("/me")
    public ResponseEntity<MyInfoResponse> updateProfile(@AuthenticationPrincipal Long userId,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Long userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request.password());
        return ResponseEntity.noContent().build(); // 204
    }
}
