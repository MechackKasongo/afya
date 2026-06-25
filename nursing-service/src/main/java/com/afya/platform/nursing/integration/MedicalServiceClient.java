package com.afya.platform.nursing.integration;

import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class MedicalServiceClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String serviceKey;

    public MedicalServiceClient(
            @Value("${app.services.medical-base-url}") String baseUrl,
            @Value("${app.internal.service-key}") String serviceKey,
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
        this.serviceKey = serviceKey;
        this.restClient = RestClients.create(
                baseUrl,
                connectTimeout,
                readTimeout,
                retryMaxAttempts,
                retryDelay,
                circuitFailureThreshold,
                circuitOpenDuration
        );
    }

    public InternalMedicalRecordSummary medicalRecordByPatient(Long patientId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/medical-records/by-patient/{patientId}", patientId)
                    .header(INTERNAL_HEADER, serviceKey)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(InternalMedicalRecordSummary.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Dossier médical introuvable pour le patient : " + patientId);
        }
    }

    public InternalPrescriptionSummary prescription(Long prescriptionLineId, String authorizationHeader) {
        try {
            return restClient.get()
                    .uri("/api/v1/internal/prescriptions/{id}", prescriptionLineId)
                    .header(INTERNAL_HEADER, serviceKey)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(InternalPrescriptionSummary.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Prescription introuvable : " + prescriptionLineId);
        }
    }

    public void completePrescription(Long prescriptionLineId) {
        try {
            restClient.post()
                    .uri("/api/v1/internal/prescriptions/{id}/complete", prescriptionLineId)
                    .header(INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new ConflictException("La prescription n'est plus active");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Prescription introuvable : " + prescriptionLineId);
        }
    }
}
