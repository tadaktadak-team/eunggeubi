package com.tadaktadak.eunggeubi.domain.emergency.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyAlertResponse(
        LocalDateTime sentAt,
        String message,
        List<GuardianResult> guardians,
        String skipReason   // 문자를 생략한 이유: RECENTLY_SENT(1분 안에 이미 보냄) / DAILY_LIMIT(하루 상한). 보냈으면 null
) {
    public record GuardianResult(
            String name,
            String phone,
            Relationship relationship,   // PARENT / GRANDPARENT / SIBLING / OTHER
            String status                // SENDING(발송 접수, 실제 도착 여부는 알 수 없음) / SKIPPED(발송 생략)
    ) {
    }
}
