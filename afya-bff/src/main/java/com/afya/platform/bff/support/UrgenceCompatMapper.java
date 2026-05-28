package com.afya.platform.bff.support;

import com.afya.platform.bff.dto.EmergencyResponse;
import com.afya.platform.bff.dto.UrgenceCompatResponse;

public final class UrgenceCompatMapper {

    private UrgenceCompatMapper() {
    }

    public static UrgenceCompatResponse toCompat(EmergencyResponse raw) {
        return new UrgenceCompatResponse(
                raw.id(),
                raw.patientId(),
                raw.triageNotes(),
                raw.priority() != null ? raw.priority() : "P2",
                raw.triageLevel(),
                raw.orientation(),
                raw.status(),
                raw.arrivedAt(),
                raw.endedAt()
        );
    }
}
