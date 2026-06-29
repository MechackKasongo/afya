package com.afya.platform.lab.integration;

import com.afya.platform.shared.http.RestClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * M1 — « transmettre les résultats » : prévient le service médical (appel interne machine-to-machine)
 * qu'un compte rendu de laboratoire est disponible, afin qu'il apparaisse dans la consultation à l'origine
 * de la demande. Best-effort : un échec ne doit jamais empêcher la saisie du résultat côté laboratoire.
 */
@Component
public class MedicalNotificationClient {

    public static final String INTERNAL_HEADER = "X-Internal-Service-Key";

    private static final Logger log = LoggerFactory.getLogger(MedicalNotificationClient.class);

    private final RestClient restClient;
    private final String serviceKey;

    public MedicalNotificationClient(
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

    public void notifyExamResultAvailable(Long examRequestId) {
        try {
            restClient.post()
                    .uri("/api/v1/internal/consultations/exam-results/{examRequestId}/ready", examRequestId)
                    .header(INTERNAL_HEADER, serviceKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn(
                    "Notification résultat labo {} au service médical impossible (sera visible via le suivi médecin) : {}",
                    examRequestId,
                    ex.getMessage());
        }
    }
}
