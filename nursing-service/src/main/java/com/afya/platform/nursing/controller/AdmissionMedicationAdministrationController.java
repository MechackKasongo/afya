package com.afya.platform.nursing.controller;

import com.afya.platform.nursing.dto.AdmissionMedicationAdministrationCreateRequest;
import com.afya.platform.nursing.dto.AdmissionMedicationAdministrationResponse;
import com.afya.platform.nursing.service.AdmissionMedicationAdministrationService;
import com.afya.platform.nursing.service.AuthorizationHeaderSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions/{admissionId}/prescription-lines/{lineId}/administrations")
public class AdmissionMedicationAdministrationController {

        private final AdmissionMedicationAdministrationService administrationService;

        public AdmissionMedicationAdministrationController(
                        AdmissionMedicationAdministrationService administrationService) {
                this.administrationService = administrationService;
        }

        @GetMapping
        public List<AdmissionMedicationAdministrationResponse> list(
                        @PathVariable Long admissionId,
                        @PathVariable Long lineId,
                        HttpServletRequest httpRequest) {
                return administrationService.listByAdmissionLine(
                                admissionId,
                                lineId,
                                AuthorizationHeaderSupport.requireBearer(httpRequest));
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public AdmissionMedicationAdministrationResponse create(
                        @PathVariable Long admissionId,
                        @PathVariable Long lineId,
                        Authentication auth,
                        HttpServletRequest httpRequest,
                        @Valid @RequestBody AdmissionMedicationAdministrationCreateRequest request) {
                return administrationService.record(
                                admissionId,
                                lineId,
                                request,
                                auth.getName(),
                                AuthorizationHeaderSupport.requireBearer(httpRequest));
        }
}
