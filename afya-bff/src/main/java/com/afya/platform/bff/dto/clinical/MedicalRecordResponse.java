package com.afya.platform.bff.dto.clinical;

import java.time.Instant;
import java.util.List;

public record MedicalRecordResponse(
        Long id,
        Long patientId,
        String patientName,
        String dossierNumber,
        Instant openedAt,
        String allergies,
        String antecedents,
        List<ClinicalNoteResponse> notes,
        List<DiagnosisResponse> diagnoses,
        List<PrescriptionLineResponse> prescriptions,
        List<NursingCareResponse> nursingCare,
        List<ClinicalDocumentResponse> documents
) {
}
