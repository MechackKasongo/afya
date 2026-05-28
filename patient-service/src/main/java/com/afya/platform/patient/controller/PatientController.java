package com.afya.platform.patient.controller;

import com.afya.platform.patient.dto.DeathDeclarationRequest;
import com.afya.platform.patient.dto.PatientContactsUpdateRequest;
import com.afya.platform.patient.dto.PatientCreateRequest;
import com.afya.platform.patient.dto.PatientResponse;
import com.afya.platform.patient.dto.PatientUpdateRequest;
import com.afya.platform.patient.service.PatientRegistryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientRegistryService patientRegistryService;

    public PatientController(PatientRegistryService patientRegistryService) {
        this.patientRegistryService = patientRegistryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientCreateRequest request) {
        return patientRegistryService.create(request);
    }

    @GetMapping("/{id}")
    public PatientResponse getById(@PathVariable Long id) {
        return patientRegistryService.getById(id);
    }

    @GetMapping
    public Page<PatientResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        return patientRegistryService.search(query, page, size, sortBy, sortDir);
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientUpdateRequest request) {
        return patientRegistryService.update(id, request);
    }

    @PatchMapping("/{id}/contacts")
    public PatientResponse updateContacts(
            @PathVariable Long id,
            @Valid @RequestBody PatientContactsUpdateRequest request
    ) {
        return patientRegistryService.updateContacts(id, request);
    }

    @PutMapping("/{id}/declare-death")
    public PatientResponse declareDeath(
            @PathVariable Long id,
            @RequestBody(required = false) DeathDeclarationRequest request
    ) {
        return patientRegistryService.declareDeath(id, request);
    }
}
