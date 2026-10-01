package com.tadaktadak.eunggeubi.domain.user.service;

import com.tadaktadak.eunggeubi.domain.auth.service.GuardianConsentService;
import com.tadaktadak.eunggeubi.domain.user.dto.GuardianRequest;
import com.tadaktadak.eunggeubi.domain.user.dto.GuardianResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.util.PhoneNumbers;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianService {

    private static final int MAX_GUARDIANS = 5;

    private final GuardianRepository guardianRepository;
    private final UserRepository userRepository;
    private final GuardianConsentService guardianConsentService;

    public List<GuardianResponse> getGuardians(Long userId) {
        return guardianRepository.findByUserId(userId).stream()
                .map(GuardianResponse::from)
                .toList();
    }

    @Transactional
    public GuardianResponse addGuardian(Long userId, GuardianRequest request) {
        if (guardianRepository.countByUserId(userId) >= MAX_GUARDIANS) {
            throw new IllegalArgumentException("보호자는 최대 " + MAX_GUARDIANS + "명까지 등록할 수 있어요.");
        }
        validatePhone(userId, request.phone(), null);

        Guardian guardian = guardianRepository.save(Guardian.builder()
                .userId(userId)
                .name(request.name())
                .phone(request.phone())
                .relationship(request.relationship())
                .notifyEnabled(request.notifyEnabled() == null || request.notifyEnabled())
                .createdAt(LocalDateTime.now())
                .build());
        guardianConsentService.requestVerification(guardian);
        return GuardianResponse.from(guardian);
    }

    @Transactional
    public GuardianResponse updateGuardian(Long userId, Long guardianId, GuardianRequest request) {
        Guardian guardian = findOwned(userId, guardianId);
        boolean phoneChanged = !PhoneNumbers.digitsOnly(guardian.getPhone()).equals(request.phone());
        if (phoneChanged) {
            validatePhone(userId, request.phone(), guardianId);
        }

        guardian.update(
                request.name(),
                request.phone(),
                request.relationship(),
                request.notifyEnabled() == null || request.notifyEnabled());
        if (phoneChanged) {
            guardianConsentService.requestVerification(guardian);
        }
        return GuardianResponse.from(guardian); // 변경 감지로 자동 UPDATE
    }

    @Transactional
    public GuardianResponse resendVerification(Long userId, Long guardianId) {
        Guardian guardian = findOwned(userId, guardianId);
        if (guardian.isVerified()) {
            throw new IllegalArgumentException("이미 동의를 받은 보호자예요.");
        }
        guardianConsentService.requestVerification(guardian);
        return GuardianResponse.from(guardian);
    }

    @Transactional
    public void deleteGuardian(Long userId, Long guardianId) {
        guardianRepository.delete(findOwned(userId, guardianId));
    }

    // 본인 번호나 이미 등록한 번호는 보호자로 둘 수 없다
    private void validatePhone(Long userId, String phone, Long excludeGuardianId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if (PhoneNumbers.digitsOnly(user.getPhone()).equals(phone)) {
            throw new IllegalArgumentException("본인 번호는 보호자로 등록할 수 없어요.");
        }
        boolean duplicated = excludeGuardianId == null
                ? guardianRepository.existsByUserIdAndPhone(userId, phone)
                : guardianRepository.existsByUserIdAndPhoneAndIdNot(userId, phone, excludeGuardianId);
        if (duplicated) {
            throw new IllegalArgumentException("이미 등록된 보호자 번호예요.");
        }
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
