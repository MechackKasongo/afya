package com.afya.platform.clinical.controller;

import com.afya.platform.clinical.dto.*;
import com.afya.platform.clinical.service.AuthorizationHeaderSupport;
import com.afya.platform.clinical.service.ClinicalRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
public class MedicalRecordController {

    private final ClinicalRecordService clinicalRecordService;

    public MedicalRecordController(ClinicalRecordService clinicalRecordService) {
        this.clinicalRecordService = clinicalRecordService;
    }

    @PatchMapping("/medical-record/allergies")
    public MedicalRecordResponse updateAllergies(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody MedicalRecordAllergiesUpdateRequest request
    ) {
        return clinicalRecordService.updateAllergies(
                patientId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PatchMapping("/medical-record/antecedents")
    public MedicalRecordResponse updateAntecedents(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody MedicalRecordAntecedentsUpdateRequest request
    ) {
        return clinicalRecordService.updateAntecedents(
                patientId,
                request,
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/medical-record")
    public MedicalRecordResponse getMedicalRecord(
            @PathVariable Long patientId,
            @RequestParam(required = false, defaultValue = "false") boolean activePrescriptionsOnly,
            HttpServletRequest httpRequest
    ) {
        return clinicalRecordService.getMedicalRecord(
                patientId,
                AuthorizationHeaderSupport.requireBearer(httpRequest),
                activePrescriptionsOnly
        );
    }

    @PostMapping("/medical-record/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalNoteResponse addNote(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody ClinicalNoteRequest request
    ) {
        return clinicalRecordService.addNote(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/medical-record/diagnoses")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse addDiagnosis(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody DiagnosisRequest request
    ) {
        return clinicalRecordService.addDiagnosis(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/prescriptions")
    public List<PrescriptionLineResponse> listPrescriptions(
            @PathVariable Long patientId,
            @RequestParam(required = false) Boolean activeOnly,
            HttpServletRequest httpRequest
    ) {
        return clinicalRecordService.listPrescriptions(
                patientId, activeOnly, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionLineResponse addPrescription(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody PrescriptionCreateRequest request
    ) {
        return clinicalRecordService.addPrescription(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/nursing-care")
    public List<NursingCareResponse> listNursingCare(
            @PathVariable Long patientId,
            HttpServletRequest httpRequest
    ) {
        return clinicalRecordService.listNursingCare(
                patientId, AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/nursing-care")
    @ResponseStatus(HttpStatus.CREATED)
    public NursingCareResponse addNursingCare(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody NursingCareRequest request
    ) {
        return clinicalRecordService.addNursingCare(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalDocumentResponse addDocument(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody ClinicalDocumentRequest request
    ) {
        return clinicalRecordService.addDocument(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalDocumentResponse uploadDocument(
            @PathVariable Long patientId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            Authentication auth,
            HttpServletRequest httpRequest
    ) throws Exception {
        return clinicalRecordService.uploadDocument(
                patientId,
                title,
                file.getContentType(),
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getSize(),
                auth.getName(),
                AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable Long patientId,
            @PathVariable Long documentId,
            HttpServletRequest httpRequest
    ) {
        ClinicalRecordService.DownloadedClinicalDocument downloaded = clinicalRecordService.downloadDocument(
                patientId, documentId, AuthorizationHeaderSupport.requireBearer(httpRequest));
        String filename = downloaded.title().replace("\"", "'");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .body(new InputStreamResource(downloaded.stream()));
    }
}
