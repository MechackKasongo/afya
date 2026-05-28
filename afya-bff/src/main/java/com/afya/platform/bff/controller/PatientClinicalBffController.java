package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.ClinicalRecordClient;
import com.afya.platform.bff.dto.ConsultationEventResponse;
import com.afya.platform.bff.dto.clinical.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
public class PatientClinicalBffController {

    private final ClinicalRecordClient clinicalRecordClient;

    public PatientClinicalBffController(ClinicalRecordClient clinicalRecordClient) {
        this.clinicalRecordClient = clinicalRecordClient;
    }

    @GetMapping("/consultation-events")
    public java.util.List<ConsultationEventResponse> consultationEvents(
            @PathVariable Long patientId,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.patientClinicalTimeline(
                patientId,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/clinical-timeline")
    public java.util.List<ConsultationEventResponse> clinicalTimeline(
            @PathVariable Long patientId,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.patientClinicalTimeline(
                patientId,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/medical-record/allergies")
    public MedicalRecordResponse updateAllergies(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicalRecordAllergiesUpdateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.updateAllergies(
                patientId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PatchMapping("/medical-record/antecedents")
    public MedicalRecordResponse updateAntecedents(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicalRecordAntecedentsUpdateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.updateAntecedents(
                patientId,
                body,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/medical-record")
    public MedicalRecordResponse getMedicalRecord(
            @PathVariable Long patientId,
            @RequestParam(required = false, defaultValue = "false") boolean activePrescriptionsOnly,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.getMedicalRecord(
                patientId,
                activePrescriptionsOnly,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/medical-record/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalNoteResponse addNote(
            @PathVariable Long patientId,
            @Valid @RequestBody ClinicalNoteRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addNote(
                patientId, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/medical-record/diagnoses")
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosisResponse addDiagnosis(
            @PathVariable Long patientId,
            @Valid @RequestBody DiagnosisRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addDiagnosis(
                patientId, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionLineResponse addPrescription(
            @PathVariable Long patientId,
            @Valid @RequestBody PrescriptionCreateRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addPrescription(
                patientId, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/nursing-care")
    @ResponseStatus(HttpStatus.CREATED)
    public NursingCareResponse addNursingCare(
            @PathVariable Long patientId,
            @Valid @RequestBody NursingCareRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addNursingCare(
                patientId, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/prescriptions")
    public java.util.List<PrescriptionLineResponse> listPrescriptions(
            @PathVariable Long patientId,
            @RequestParam(required = false) Boolean activeOnly,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.listPrescriptions(
                patientId,
                activeOnly,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/nursing-care")
    public java.util.List<NursingCareResponse> listNursingCare(
            @PathVariable Long patientId,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.listNursingCare(
                patientId, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalDocumentResponse addDocument(
            @PathVariable Long patientId,
            @Valid @RequestBody ClinicalDocumentRequest body,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.addDocument(
                patientId, body, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalDocumentResponse uploadDocument(
            @PathVariable Long patientId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title", required = false) String title,
            HttpServletRequest request
    ) {
        return clinicalRecordClient.uploadDocument(
                patientId,
                file,
                title,
                AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long patientId,
            @PathVariable Long documentId,
            HttpServletRequest request
    ) {
        byte[] body = clinicalRecordClient.downloadDocument(
                patientId, documentId, AuthorizationSupport.requireBearer(request.getHeader("Authorization")));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-" + documentId + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}
