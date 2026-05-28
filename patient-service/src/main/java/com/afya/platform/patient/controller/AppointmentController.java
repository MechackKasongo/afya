package com.afya.platform.patient.controller;

import com.afya.platform.patient.dto.AppointmentCreateRequest;
import com.afya.platform.patient.dto.AppointmentResponse;
import com.afya.platform.patient.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public List<AppointmentResponse> list(@PathVariable Long patientId) {
        return appointmentService.listByPatient(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(
            @PathVariable Long patientId,
            @Valid @RequestBody AppointmentCreateRequest request
    ) {
        return appointmentService.create(patientId, request);
    }

    @PostMapping("/{appointmentId}/cancel")
    public AppointmentResponse cancel(@PathVariable Long patientId, @PathVariable Long appointmentId) {
        return appointmentService.cancel(patientId, appointmentId);
    }
}
