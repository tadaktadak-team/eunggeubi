package com.tadaktadak.eunggeubi.domain.user.controller;

import com.tadaktadak.eunggeubi.domain.user.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.GuardianResponse;
import com.tadaktadak.eunggeubi.domain.user.service.GuardianService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @GetMapping
    public ResponseEntity<List<GuardianResponse>> getGuardians(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(guardianService.getGuardians(userId));
    }

    @PostMapping
    public ResponseEntity<GuardianResponse> addGuardian(
            @AuthenticationPrincipal Long userId, @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardianService.addGuardian(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuardianResponse> updateGuardian(
            @AuthenticationPrincipal Long userId, @PathVariable Long id,
            @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(guardianService.updateGuardian(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuardian(
            @AuthenticationPrincipal Long userId, @PathVariable Long id) {
        guardianService.deleteGuardian(userId, id);
        return ResponseEntity.noContent().build();
    }
}
