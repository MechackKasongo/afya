package com.afya.platform.admission.stay.dto;

import jakarta.validation.constraints.Size;

public record HospitalizationFormRequest(
        @Size(max = 16000) String antecedentsText,
        @Size(max = 16000) String anamnesisText,
        @Size(max = 8000) String physicalExamPulmonaryText,
        @Size(max = 8000) String physicalExamCardiacText,
        @Size(max = 8000) String physicalExamAbdominalText,
        @Size(max = 8000) String physicalExamNeurologicalText,
        @Size(max = 8000) String physicalExamMiscText,
        @Size(max = 16000) String paraclinicalText,
        @Size(max = 8000) String conclusionText
) {
}
