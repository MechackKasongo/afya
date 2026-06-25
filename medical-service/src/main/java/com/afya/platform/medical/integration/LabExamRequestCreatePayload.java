package com.afya.platform.medical.integration;

import java.util.List;

public record LabExamRequestCreatePayload(
        Long patientId,
        Long doctorId,
        Long admissionId,
        String urgency,
        String comment,
        List<Long> examTypeIds
) {
}
