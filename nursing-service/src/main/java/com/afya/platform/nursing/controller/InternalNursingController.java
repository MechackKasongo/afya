package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.CreatePrescriptionNotificationRequest;
import com.afya.platform.nursing.dto.NursingCareResponse;
import com.afya.platform.nursing.dto.PrescriptionNotificationResponse;
import com.afya.platform.nursing.service.NursingCareService;
import com.afya.platform.nursing.service.PrescriptionNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal")
public class InternalNursingController {

    private final NursingCareService nursingCareService;
    private final PrescriptionNotificationService prescriptionNotificationService;

    public InternalNursingController(
            NursingCareService nursingCareService,
            PrescriptionNotificationService prescriptionNotificationService
    ) {
        this.nursingCareService = nursingCareService;
        this.prescriptionNotificationService = prescriptionNotificationService;
    }

    @GetMapping("/nursing-care/by-medical-record/{medicalRecordId}")
    public List<NursingCareResponse> nursingCareByMedicalRecord(@PathVariable Long medicalRecordId) {
        return nursingCareService.listNursingCareByMedicalRecord(medicalRecordId);
    }

    @GetMapping("/administered-prescription-line-ids")
    public List<Long> administeredLineIds(@RequestParam Long medicalRecordId) {
        return nursingCareService.administeredPrescriptionLineIds(medicalRecordId);
    }

    @PostMapping("/prescription-notifications")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionNotificationResponse createPrescriptionNotification(
            @Valid @RequestBody CreatePrescriptionNotificationRequest request
    ) {
        return prescriptionNotificationService.create(request);
    }
}
