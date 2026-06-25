package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.VitalSignAlertResponse;
import com.afya.platform.bff.dto.VitalSignCreateRequest;
import com.afya.platform.bff.dto.VitalSignResponse;
import com.afya.platform.bff.dto.clinical.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class NursingClient {

    private final RestClient restClient;

    public NursingClient(
            @Value("${app.services.nursing-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
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

    public List<NursingCareResponse> listNursingCare(Long patientId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/nursing-care", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(NursingCareResponse.class));
    }

    public List<VitalSignResponse> listVitalSigns(Long admissionId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/admissions/{admissionId}/vital-signs", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(VitalSignResponse.class));
    }

    public VitalSignResponse createVitalSign(
            Long admissionId,
            VitalSignCreateRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/admissions/{admissionId}/vital-signs", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(VitalSignResponse.class);
    }

    public List<VitalSignAlertResponse> listVitalSignAlerts(Long admissionId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/admissions/{admissionId}/vital-sign-alerts", admissionId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(VitalSignAlertResponse.class));
    }

    public List<PrescriptionNotificationResponse> listPrescriptionNotifications(
            Long patientId,
            String authorizationHeader
    ) {
        return restClient.get()
                .uri("/api/v1/patients/{patientId}/prescription-notifications", patientId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(PrescriptionNotificationResponse.class));
    }

    public PrescriptionNotificationResponse markPrescriptionNotificationRead(
            Long patientId,
            Long notificationId,
            String authorizationHeader
    ) {
        return restClient.patch()
                .uri("/api/v1/patients/{patientId}/prescription-notifications/{notificationId}/read",
                        patientId, notificationId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(PrescriptionNotificationResponse.class);
    }
}
