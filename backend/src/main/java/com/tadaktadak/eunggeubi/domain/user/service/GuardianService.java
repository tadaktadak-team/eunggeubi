package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.user.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.GuardianResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private final GuardianRepository guardianRepository;

    public List<GuardianResponse> getGuardians(Long userId) {
        return guardianRepository.findByUserId(userId).stream()
                .map(GuardianResponse::from)
                .toList();
    }

    @Transactional
    public GuardianResponse addGuardian(Long userId, GuardianRequest request) {
        Guardian guardian = Guardian.builder()
                .userId(userId)
                .name(request.name())
                .phone(request.phone())
                .relationship(request.relationship())
                .notifyEnabled(request.notifyEnabled() == null || request.notifyEnabled())
                .createdAt(LocalDateTime.now())
                .build();
        return GuardianResponse.from(guardianRepository.save(guardian));
    }

    @Transactional
    public GuardianResponse updateGuardian(Long userId, Long guardianId, GuardianRequest request) {
        Guardian guardian = findOwned(userId, guardianId);
        guardian.update(
                request.name(),
                request.phone(),
                request.relationship(),
                request.notifyEnabled() == null || request.notifyEnabled());
        return GuardianResponse.from(guardian); // 변경 감지로 자동 UPDATE
    }

    @Transactional
    public void deleteGuardian(Long userId, Long guardianId) {
        guardianRepository.delete(findOwned(userId, guardianId));
    }

    // 내 보호자인지 확인 (남의 보호자 접근 차단)
    private Guardian findOwned(Long userId, Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));
        if (!guardian.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 보호자만 접근할 수 있습니다.");
        }
        return guardian;
    }
}
