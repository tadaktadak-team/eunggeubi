package com.tadaktadak.eunggeubi.domain.emergency.service;

import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertRequest;
import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final SmsSender smsSender;

    public EmergencyAlertResponse sendAlert(Long userId, EmergencyAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        List<Guardian> guardians = guardianRepository.findByUserIdAndNotifyEnabledTrue(userId);

        String mapLink = "https://map.kakao.com/link/map/현재위치,"
                + request.latitude() + "," + request.longitude();
        String message = user.getName()
                + " 님이 응급 상황으로 서비스 이용 중, 현재 위치: " + mapLink;

        List<EmergencyAlertResponse.GuardianResult> results = guardians.stream()
                .map(guardian -> sendToGuardian(guardian, message))
                .toList();

        return new EmergencyAlertResponse(LocalDateTime.now(), message, results);
    }

    // 보호자 개별 전송
    private EmergencyAlertResponse.GuardianResult sendToGuardian(Guardian guardian, String message) {
        String status;
        try {
            smsSender.send(guardian.getPhone(), message);
            status = "SENT";
        } catch (Exception e) {
            status = "FAILED";
        }
        return new EmergencyAlertResponse.GuardianResult(
                guardian.getName(),
                guardian.getPhone(),
                guardian.getRelationship(),
                status
        );
    }
}