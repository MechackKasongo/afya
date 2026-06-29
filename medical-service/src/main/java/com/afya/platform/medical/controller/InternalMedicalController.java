package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.InternalMedicalRecordSummary;
import com.afya.platform.medical.dto.InternalPrescriptionSummary;
import com.afya.platform.medical.service.AuthorizationHeaderSupport;
import com.afya.platform.medical.service.ConsultationService;
import com.afya.platform.medical.service.MedicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalMedicalController {

    private final MedicalRecordService medicalRecordService;
    private final ConsultationService consultationService;

    public InternalMedicalController(
            MedicalRecordService medicalRecordService,
            ConsultationService consultationService
    ) {
        this.medicalRecordService = medicalRecordService;
        this.consultationService = consultationService;
    }

    @GetMapping("/medical-records/by-patient/{patientId}")
    public InternalMedicalRecordSummary medicalRecordByPatient(
            @PathVariable Long patientId,
            HttpServletRequest httpRequest
    ) {
        return medicalRecordService.internalMedicalRecordByPatient(
                patientId,
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/prescriptions/{id}")
    public InternalPrescriptionSummary prescription(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        return medicalRecordService.internalPrescription(id, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/prescriptions/{id}/complete")
    public void completePrescription(@PathVariable Long id) {
        medicalRecordService.completePrescription(id);
    }

    /** M1 — le lab-service signale qu'un compte rendu est disponible pour une demande d'examen. */
    @PostMapping("/consultations/exam-results/{examRequestId}/ready")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markExamResultAvailable(@PathVariable Long examRequestId) {
        consultationService.markExamResultAvailable(examRequestId);
    }
}
