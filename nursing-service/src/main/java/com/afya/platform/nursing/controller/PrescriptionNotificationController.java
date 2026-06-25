package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.PrescriptionNotificationResponse;
import com.afya.platform.nursing.service.PrescriptionNotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/prescription-notifications")
public class PrescriptionNotificationController {

    private final PrescriptionNotificationService prescriptionNotificationService;

    public PrescriptionNotificationController(PrescriptionNotificationService prescriptionNotificationService) {
        this.prescriptionNotificationService = prescriptionNotificationService;
    }

    @GetMapping
    public List<PrescriptionNotificationResponse> list(@PathVariable Long patientId) {
        return prescriptionNotificationService.listByPatient(patientId);
    }

    @PatchMapping("/{notificationId}/read")
    public PrescriptionNotificationResponse markRead(
            @PathVariable Long patientId,
            @PathVariable Long notificationId,
            Authentication auth
    ) {
        return prescriptionNotificationService.markRead(patientId, notificationId, auth.getName());
    }
}
