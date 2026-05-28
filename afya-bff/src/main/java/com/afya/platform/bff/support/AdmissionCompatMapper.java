package com.afya.platform.bff.support;

import com.afya.platform.bff.dto.AdmissionResponse;
import com.afya.platform.bff.dto.StayResponse;

public final class AdmissionCompatMapper {

    private AdmissionCompatMapper() {
    }

    public static AdmissionUiResponse toUi(AdmissionResponse raw, StayResponse stay) {
        String room = stay != null ? stay.roomLabel() : null;
        String bed = stay != null ? stay.bedLabel() : null;
        String reason = firstNonBlank(raw.admissionReason(), raw.dischargeReason());
        return new AdmissionUiResponse(
                raw.id(),
                raw.patientId(),
                raw.hospitalServiceName(),
                room,
                bed,
                reason != null ? reason : "",
                raw.admittedAt(),
                raw.dischargedAt(),
                mapStatus(raw.status())
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String mapStatus(String status) {
        if (status == null) {
            return "EN_COURS";
        }
        return switch (status.toUpperCase()) {
            case "OUVERTE" -> "EN_COURS";
            case "TRANSFEREE" -> "TRANSFERE";
            case "SORTIE" -> "SORTI";
            case "ANNULEE" -> "SORTI";
            default -> status;
        };
    }

    public record AdmissionUiResponse(
            Long id,
            Long patientId,
            String serviceName,
            String room,
            String bed,
            String reason,
            java.time.Instant admissionDateTime,
            java.time.Instant dischargeDateTime,
            String status
    ) {
    }
}
