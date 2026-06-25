package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.*;
import com.afya.platform.medical.service.AuthorizationHeaderSupport;
import com.afya.platform.medical.service.MedicalRecordService;
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

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PatchMapping("/medical-record/allergies")
    public MedicalRecordResponse updateAllergies(
            @PathVariable Long patientId,
            Authentication auth,
            HttpServletRequest httpRequest,
            @Valid @RequestBody MedicalRecordAllergiesUpdateRequest request
    ) {
        return medicalRecordService.updateAllergies(
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
        return medicalRecordService.updateAntecedents(
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
        return medicalRecordService.getMedicalRecord(
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
        return medicalRecordService.addNote(
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
        return medicalRecordService.addDiagnosis(
                patientId, request, auth.getName(), AuthorizationHeaderSupport.requireBearer(httpRequest));
    }

    @GetMapping("/prescriptions")
    public List<PrescriptionLineResponse> listPrescriptions(
            @PathVariable Long patientId,
            @RequestParam(required = false) Boolean activeOnly,
            HttpServletRequest httpRequest
    ) {
        return medicalRecordService.listPrescriptions(
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
        return medicalRecordService.addPrescription(
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
        return medicalRecordService.addDocument(
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
        return medicalRecordService.uploadDocument(
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
        MedicalRecordService.DownloadedClinicalDocument downloaded = medicalRecordService.downloadDocument(
                patientId, documentId, AuthorizationHeaderSupport.requireBearer(httpRequest));
        String filename = downloaded.title().replace("\"", "'");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .body(new InputStreamResource(downloaded.stream()));
    }
}
