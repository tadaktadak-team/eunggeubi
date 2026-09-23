package com.tadaktadak.eunggeubi.domain.emergency.service;

import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertRequest;
import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.sms.SmsSender;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final UserRepository userRepository;
    private final GuardianRepository guardianRepository;
    private final SmsSender smsSender;

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final String SMS_SUBJECT = "[응급이] 응급 상황 알림";

    public EmergencyAlertResponse sendAlert(Long userId, EmergencyAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        List<Guardian> guardians = guardianRepository.findByUserIdAndNotifyEnabledTrue(userId);

        LocalDateTime sentAt = LocalDateTime.now();
        String mapLink = "https://map.kakao.com/link/map/"
                + URLEncoder.encode(user.getName() + "님 위치", StandardCharsets.UTF_8).replace("+", "%20")
                + "," + request.latitude() + "," + request.longitude();

        String message = user.getName() + " 님이 긴급 호출을 눌렀습니다.\n"
                + "119 연결과 함께 보호자에게 위치를 보냅니다.\n\n"
                + "시각: " + sentAt.format(SENT_AT_FORMAT) + "\n"
                + "위치: " + mapLink;

        List<EmergencyAlertResponse.GuardianResult> results = guardians.stream()
                .map(guardian -> sendToGuardian(guardian, message))
                .toList();

        return new EmergencyAlertResponse(sentAt, message, results);
    }

    // 보호자 개별 전송
    private EmergencyAlertResponse.GuardianResult sendToGuardian(Guardian guardian, String message) {
        String status;
        try {
            smsSender.send(guardian.getPhone(), SMS_SUBJECT, message);
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