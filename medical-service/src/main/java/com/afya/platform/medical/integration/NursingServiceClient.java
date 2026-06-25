package com.afya.platform.medical.integration;

import com.afya.platform.medical.dto.NursingCareResponse;
import com.afya.platform.shared.http.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class NursingServiceClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private final RestClient restClient;
    private final String serviceKey;

    public NursingServiceClient(
            @Value("${app.services.nursing-base-url}") String baseUrl,
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

    public List<NursingCareResponse> nursingCareByMedicalRecord(Long medicalRecordId) {
        NursingCareResponse[] response = restClient.get()
                .uri("/api/v1/internal/nursing-care/by-medical-record/{medicalRecordId}", medicalRecordId)
                .header(INTERNAL_HEADER, serviceKey)
                .retrieve()
                .body(NursingCareResponse[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public List<Long> administeredPrescriptionLineIds(Long medicalRecordId) {
        Long[] response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/internal/administered-prescription-line-ids")
                        .queryParam("medicalRecordId", medicalRecordId)
                        .build())
                .header(INTERNAL_HEADER, serviceKey)
                .retrieve()
                .body(Long[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    public void notifyPrescriptionCreated(Long prescriptionLineId, Long patientId, String drugName) {
        restClient.post()
                .uri("/api/v1/internal/prescription-notifications")
                .header(INTERNAL_HEADER, serviceKey)
                .body(new CreatePrescriptionNotificationRequest(prescriptionLineId, patientId, drugName))
                .retrieve()
                .toBodilessEntity();
    }
}
