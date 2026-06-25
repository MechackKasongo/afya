package com.afya.platform.admission.dto;

import com.afya.platform.admission.model.DischargeType;
import jakarta.validation.constraints.Size;

public record DischargeRequest(
        DischargeType type,
        @Size(max = 2000) String postDischargeInstructions,
        @Size(max = 255) String reason
) {
    public DischargeType resolvedType() {
        return type != null ? type : DischargeType.GUERI;
    }

    public String resolvedInstructions() {
        if (postDischargeInstructions != null && !postDischargeInstructions.isBlank()) {
            return postDischargeInstructions.strip();
        }
        if (reason != null && !reason.isBlank()) {
            return reason.strip();
        }
        return null;
    }
}
