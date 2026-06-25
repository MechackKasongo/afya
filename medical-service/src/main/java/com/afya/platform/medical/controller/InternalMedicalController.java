package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.InternalMedicalRecordSummary;
import com.afya.platform.medical.dto.InternalPrescriptionSummary;
import com.afya.platform.medical.service.AuthorizationHeaderSupport;
import com.afya.platform.medical.service.MedicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalMedicalController {

    private final MedicalRecordService medicalRecordService;

    public InternalMedicalController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
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
}
