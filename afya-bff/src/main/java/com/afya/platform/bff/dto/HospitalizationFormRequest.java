package com.afya.platform.bff.dto;

import jakarta.validation.constraints.Size;

/** Alias aligné sur le formulaire clinique d'admission (proxy stay-service). */
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
