package com.tadaktadak.eunggeubi.domain.emergency.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyAlertResponse(
        LocalDateTime sentAt,
        String message,
        List<GuardianResult> guardians
) {
    public record GuardianResult(
            String name,
            String phone,
            Relationship relationship,   // PARENT / GRANDPARENT / SIBLING / OTHER
            String status                // SENT / FAILED
    ) {
    }
}