package com.afya.platform.patient.controller;

import com.afya.platform.patient.dto.EmergencyContactCreateRequest;
import com.afya.platform.patient.dto.EmergencyContactResponse;
import com.afya.platform.patient.dto.EmergencyContactUpdateRequest;
import com.afya.platform.patient.dto.MedicalAntecedentCreateRequest;
import com.afya.platform.patient.dto.MedicalAntecedentResponse;
import com.afya.platform.patient.dto.MedicalAntecedentUpdateRequest;
import com.afya.platform.patient.service.PatientClinicalProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
public class PatientClinicalProfileController {

    private final PatientClinicalProfileService patientClinicalProfileService;

    public PatientClinicalProfileController(PatientClinicalProfileService patientClinicalProfileService) {
        this.patientClinicalProfileService = patientClinicalProfileService;
    }

    @GetMapping("/medical-antecedents")
    public List<MedicalAntecedentResponse> listAntecedents(@PathVariable Long patientId) {
        return patientClinicalProfileService.listAntecedents(patientId);
    }

    @PostMapping("/medical-antecedents")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalAntecedentResponse createAntecedent(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicalAntecedentCreateRequest request
    ) {
        return patientClinicalProfileService.createAntecedent(patientId, request);
    }

    @PutMapping("/medical-antecedents/{antecedentId}")
    public MedicalAntecedentResponse updateAntecedent(
            @PathVariable Long patientId,
            @PathVariable Long antecedentId,
            @Valid @RequestBody MedicalAntecedentUpdateRequest request
    ) {
        return patientClinicalProfileService.updateAntecedent(patientId, antecedentId, request);
    }

    @DeleteMapping("/medical-antecedents/{antecedentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAntecedent(@PathVariable Long patientId, @PathVariable Long antecedentId) {
        patientClinicalProfileService.deleteAntecedent(patientId, antecedentId);
    }

    @GetMapping("/emergency-contacts")
    public List<EmergencyContactResponse> listEmergencyContacts(@PathVariable Long patientId) {
        return patientClinicalProfileService.listEmergencyContacts(patientId);
    }

    @PostMapping("/emergency-contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyContactResponse createEmergencyContact(
            @PathVariable Long patientId,
            @Valid @RequestBody EmergencyContactCreateRequest request
    ) {
        return patientClinicalProfileService.createEmergencyContact(patientId, request);
    }

    @PutMapping("/emergency-contacts/{contactId}")
    public EmergencyContactResponse updateEmergencyContact(
            @PathVariable Long patientId,
            @PathVariable Long contactId,
            @Valid @RequestBody EmergencyContactUpdateRequest request
    ) {
        return patientClinicalProfileService.updateEmergencyContact(patientId, contactId, request);
    }

    @DeleteMapping("/emergency-contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmergencyContact(@PathVariable Long patientId, @PathVariable Long contactId) {
        patientClinicalProfileService.deleteEmergencyContact(patientId, contactId);
    }
}
