package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.*;
import com.afya.platform.bff.dto.clinical.*;
import com.afya.platform.bff.support.PageRestSupport;
import org.springframework.data.domain.Page;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ClinicalRecordClient {

    private final RestClient restClient;

    public ClinicalRecordClient(
            @Value("${app.services.clinical-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public Page<ConsultationResponse> listConsultations(
            Long patientId,
            Long admissionId,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> {
                    var builder = uriBuilder.path("/api/v1/consultations");
                    if (patientId != null) {
                        builder.queryParam("patientId", patientId);
                    }
                    if (admissionId != null) {
                        builder.queryParam("admissionId", admissionId);
                    }
                    return builder
                            .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                            .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                            .queryParamIfPresent("sortBy", java.util.Optional.ofNullable(sortBy))
                            .queryParamIfPresent("sortDir", java.util.Optional.ofNullable(sortDir))
                            .build();
                },
                ConsultationResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public ConsultationResponse getConsultation(Long consultationId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/consultations/{id}", consultationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(ConsultationResponse.class);
    }

    public ConsultationResponse createConsultation(
            ConsultationCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/consultations")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ConsultationResponse.class);
    }

    public ConsultationEventResponse addConsultationObservation(
            Long consultationId,
            EventCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/consultations/{id}/observations", consultationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ConsultationEventResponse.class);
    }

    public ConsultationEventResponse addConsultationDiagnostic(
            Long consultationId,
            EventCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/consultations/{id}/diagnostics", consultationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ConsultationEventResponse.class);
    }

    public ConsultationEventResponse addConsultationExamOrder(
            Long consultationId,
            EventCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/consultations/{id}/orders/exams", consultationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ConsultationEventResponse.class);
    }

    public List<ConsultationEventResponse> patientClinicalTimeline(Long patientId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/consultation-events", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(ConsultationEventResponse.class));
    }

    public List<ConsultationEventResponse> consultationEvents(Long consultationId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/consultations/{consultationId}/events", consultationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(ConsultationEventResponse.class));
    }

    public List<DiseaseCatalogResponse> listSelectableDiseases(String diseaseType, String authorizationHeader) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/disease-catalog")
                        .queryParam("diseaseType", diseaseType)
                        .build())
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(DiseaseCatalogResponse.class));
    }

    public MedicalRecordResponse getMedicalRecord(
            Long patientId,
            boolean activePrescriptionsOnly,
            String authorizationHeader
    ) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/patients/{patientId}/medical-record")
                        .queryParam("activePrescriptionsOnly", activePrescriptionsOnly)
                        .build(patientId))
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(MedicalRecordResponse.class);
    }

    public MedicalRecordResponse updateAllergies(
            Long patientId,
            MedicalRecordAllergiesUpdateRequest request,
            String authorizationHeader
    ) {
        return restClient.patch()
                .uri("/api/v1/patients/{patientId}/medical-record/allergies", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(MedicalRecordResponse.class);
    }

    public MedicalRecordResponse updateAntecedents(
            Long patientId,
            MedicalRecordAntecedentsUpdateRequest request,
            String authorizationHeader
    ) {
        return restClient.patch()
                .uri("/api/v1/patients/{patientId}/medical-record/antecedents", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(MedicalRecordResponse.class);
    }

    public ClinicalNoteResponse addNote(
            Long patientId,
            ClinicalNoteRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/medical-record/notes", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ClinicalNoteResponse.class);
    }

    public DiagnosisResponse addDiagnosis(
            Long patientId,
            DiagnosisRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/medical-record/diagnoses", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(DiagnosisResponse.class);
    }

    public PrescriptionLineResponse addPrescription(
            Long patientId,
            PrescriptionCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/prescriptions", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(PrescriptionLineResponse.class);
    }

    public NursingCareResponse addNursingCare(
            Long patientId,
            NursingCareRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/nursing-care", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(NursingCareResponse.class);
    }

    public MedicationAdministrationResponse administer(
            Long prescriptionLineId,
            MedicationAdministrationRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/prescriptions/{prescriptionLineId}/administrations", prescriptionLineId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request != null ? request : new MedicationAdministrationRequest(null, null))
                .retrieve()
                .body(MedicationAdministrationResponse.class);
    }

    public List<PrescriptionLineResponse> listPrescriptions(
            Long patientId,
            Boolean activeOnly,
            String authorizationHeader
    ) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/patients/{patientId}/prescriptions")
                        .queryParamIfPresent("activeOnly", java.util.Optional.ofNullable(activeOnly))
                        .build(patientId))
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(PrescriptionLineResponse.class));
    }

    public List<NursingCareResponse> listNursingCare(Long patientId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/nursing-care", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(NursingCareResponse.class));
    }

    public ClinicalDocumentResponse addDocument(
            Long patientId,
            ClinicalDocumentRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/patients/{patientId}/documents", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ClinicalDocumentResponse.class);
    }

    public ClinicalVolumesResponse volumes(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stats/volumes")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(ClinicalVolumesResponse.class);
    }

    public ClinicalDocumentResponse uploadDocument(
            Long patientId,
            org.springframework.web.multipart.MultipartFile file,
            String title,
            String authorizationHeader
    ) {
        try {
            org.springframework.util.LinkedMultiValueMap<String, Object> parts =
                    new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
                }
            });
            if (title != null && !title.isBlank()) {
                parts.add("title", title);
            }
            return restClient.post()
                    .uri("/api/v1/patients/{patientId}/documents/upload", patientId)
                    .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(ClinicalDocumentResponse.class);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Lecture du fichier impossible", e);
        }
    }

    public byte[] downloadDocument(Long patientId, Long documentId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/documents/{documentId}/download", patientId, documentId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(byte[].class);
    }
}
