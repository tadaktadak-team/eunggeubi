package com.tadaktadak.eunggeubi.domain.emergency.service;

import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertRequest;
import com.tadaktadak.eunggeubi.domain.emergency.dto.EmergencyAlertResponse;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.User;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;

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
    private final EmergencyAlertNotifier notifier;

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final String SMS_SUBJECT = "[응급이] 응급 상황 알림";
    private static final int ADDRESS_MAX_LENGTH = 100;

    public EmergencyAlertResponse sendAlert(Long userId, EmergencyAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        List<Guardian> guardians = guardianRepository.findByUserIdAndNotifyEnabledTrueAndVerifiedAtIsNotNull(userId);

        LocalDateTime sentAt = LocalDateTime.now();
        String mapLink = "https://map.kakao.com/link/map/"
                + URLEncoder.encode(user.getName() + "님 위치", StandardCharsets.UTF_8).replace("+", "%20")
                + "," + request.latitude() + "," + request.longitude();

        String address = sanitizeAddress(request.address());
        String where = address.isEmpty()
                ? "위치: " + mapLink
                : "위치: " + address + "\n지도: " + mapLink;

        String message = user.getName() + " 님이 긴급 호출을 눌렀습니다.\n"
                + "119 연결과 함께 보호자에게 위치를 보냅니다.\n\n"
                + "시각: " + sentAt.format(SENT_AT_FORMAT) + "\n"
                + where;

        for (Guardian guardian : guardians) {
            notifier.notifyGuardian(guardian.getPhone(), SMS_SUBJECT, message);
        }

        List<EmergencyAlertResponse.GuardianResult> results = guardians.stream()
                .map(this::toSendingResult)
                .toList();

        return new EmergencyAlertResponse(sentAt, message, results);
    }

    // 응급 상황에서 주소가 길다는 이유로 알림이 막히면 안 되므로 거절하지 않고 한 줄 100자로 정리한다
    private String sanitizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String oneLine = address.replaceAll("[\\r\\n\\t]+", " ").trim();
        return oneLine.length() > ADDRESS_MAX_LENGTH ? oneLine.substring(0, ADDRESS_MAX_LENGTH) : oneLine;
    }

    // 발송은 비동기라 응답 시점에는 결과를 알 수 없다. 접수 사실만 돌려준다.
    private EmergencyAlertResponse.GuardianResult toSendingResult(Guardian guardian) {
        return new EmergencyAlertResponse.GuardianResult(
                guardian.getName(),
                guardian.getPhone(),
                guardian.getRelationship(),
                "SENDING"
        );
    }
}