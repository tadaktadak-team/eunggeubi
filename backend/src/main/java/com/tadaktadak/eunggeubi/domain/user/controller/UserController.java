package com.tadaktadak.eunggeubi.domain.user.controller;

import com.tadaktadak.eunggeubi.domain.user.dto.ProfileResponse;
import com.tadaktadak.eunggeubi.domain.user.dto.UpdateProfileRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.WithdrawRequest;
import com.tadaktadak.eunggeubi.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId,
                                         @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request.password());
        return ResponseEntity.noContent().build(); // 204
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PostMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(@AuthenticationPrincipal Long userId,
                                                         @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}