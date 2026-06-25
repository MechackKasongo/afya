package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.PatientClient;
import com.afya.platform.bff.dto.DeathDeclarationRequest;
import com.afya.platform.bff.dto.EmergencyContactCreateRequest;
import com.afya.platform.bff.dto.EmergencyContactResponse;
import com.afya.platform.bff.dto.EmergencyContactUpdateRequest;
import com.afya.platform.bff.dto.MedicalAntecedentCreateRequest;
import com.afya.platform.bff.dto.MedicalAntecedentResponse;
import com.afya.platform.bff.dto.MedicalAntecedentUpdateRequest;
import com.afya.platform.bff.dto.PatientCreateRequest;
import com.afya.platform.bff.dto.PatientResponse;
import com.afya.platform.bff.dto.PatientContactsUpdateRequest;
import com.afya.platform.bff.dto.PatientUpdateRequest;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientBffController {

    private final PatientClient patientClient;

    public PatientBffController(PatientClient patientClient) {
        this.patientClient = patientClient;
    }

    @GetMapping
    public Page<PatientResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        return patientClient.search(
                query,
                page,
                size,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable Long id, HttpServletRequest request) {
        return patientClient.getById(id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(
            @Valid @RequestBody PatientCreateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.create(body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}")
    public PatientResponse update(
            @PathVariable Long id,
            @Valid @RequestBody PatientUpdateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.update(id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/{id}/contacts")
    public PatientResponse updateContacts(
            @PathVariable Long id,
            @Valid @RequestBody PatientContactsUpdateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.updateContacts(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}/declare-death")
    public PatientResponse declareDeath(
            @PathVariable Long id,
            @RequestBody(required = false) DeathDeclarationRequest body,
            HttpServletRequest request
    ) {
        return patientClient.declareDeath(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/medical-antecedents")
    public List<MedicalAntecedentResponse> listMedicalAntecedents(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return patientClient.listMedicalAntecedents(
                id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/medical-antecedents")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalAntecedentResponse createMedicalAntecedent(
            @PathVariable Long id,
            @Valid @RequestBody MedicalAntecedentCreateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.createMedicalAntecedent(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}/medical-antecedents/{antecedentId}")
    public MedicalAntecedentResponse updateMedicalAntecedent(
            @PathVariable Long id,
            @PathVariable Long antecedentId,
            @Valid @RequestBody MedicalAntecedentUpdateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.updateMedicalAntecedent(
                id,
                antecedentId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}/medical-antecedents/{antecedentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedicalAntecedent(
            @PathVariable Long id,
            @PathVariable Long antecedentId,
            HttpServletRequest request
    ) {
        patientClient.deleteMedicalAntecedent(
                id, antecedentId, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/{id}/emergency-contacts")
    public List<EmergencyContactResponse> listEmergencyContacts(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return patientClient.listEmergencyContacts(
                id, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/{id}/emergency-contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyContactResponse createEmergencyContact(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyContactCreateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.createEmergencyContact(
                id, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PutMapping("/{id}/emergency-contacts/{contactId}")
    public EmergencyContactResponse updateEmergencyContact(
            @PathVariable Long id,
            @PathVariable Long contactId,
            @Valid @RequestBody EmergencyContactUpdateRequest body,
            HttpServletRequest request
    ) {
        return patientClient.updateEmergencyContact(
                id,
                contactId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @DeleteMapping("/{id}/emergency-contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmergencyContact(
            @PathVariable Long id,
            @PathVariable Long contactId,
            HttpServletRequest request
    ) {
        patientClient.deleteEmergencyContact(
                id, contactId, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }
}
