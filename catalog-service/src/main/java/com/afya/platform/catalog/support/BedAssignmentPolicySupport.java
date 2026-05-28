package com.afya.platform.catalog.support;

import com.afya.platform.catalog.model.BedAssignmentPolicy;
import com.afya.platform.shared.exception.BadRequestException;

public final class BedAssignmentPolicySupport {

    private BedAssignmentPolicySupport() {
    }

    public static BedAssignmentPolicy resolve(String value, BedAssignmentPolicy defaultPolicy) {
        if (value == null || value.isBlank()) {
            return defaultPolicy;
        }
        try {
            return BedAssignmentPolicy.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Politique d'attribution invalide : " + value + " (ROOM_ORDER_ASC ou LONGEST_IDLE)");
        }
    }
}
