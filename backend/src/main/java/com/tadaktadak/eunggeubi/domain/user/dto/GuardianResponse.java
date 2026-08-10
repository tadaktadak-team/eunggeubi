package com.tadaktadak.eunggeubi.domain.user.dto;

import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.entity.Relationship;

public record GuardianResponse(
        Long id,
        String name,
        String phone,
        Relationship relationship,
        boolean notifyEnabled
) {
    public static GuardianResponse from(Guardian g) {
        return new GuardianResponse(
                g.getId(), g.getName(), g.getPhone(), g.getRelationship(), g.isNotifyEnabled());
    }
}
