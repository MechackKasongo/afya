package com.afya.platform.admission.integration;

import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import com.afya.platform.shared.exception.NotFoundException;
import com.afya.platform.shared.http.RestClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class CatalogServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceClient.class);

    private final RestClient restClient;

    public CatalogServiceClient(
            @Value("${app.services.hospital-base-url}") String baseUrl,
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
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

    public HospitalServiceSummary getHospitalService(Long serviceId, String authorizationHeader) {
        try {
            HospitalServiceSummary summary = restClient.get()
                    .uri("/api/v1/hospital-services/{id}", serviceId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(HospitalServiceSummary.class);
            if (summary == null) {
                throw new NotFoundException("Service hospitalier introuvable : " + serviceId);
            }
            if (!summary.active()) {
                throw new BadRequestException("Le service hospitalier est inactif");
            }
            return summary;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Service hospitalier introuvable : " + serviceId);
        }
    }

    public BedAssignment resolveBedAssignment(
            Long hospitalServiceId,
            String roomLabel,
            String bedLabel,
            String authorizationHeader
    ) {
        if (roomLabel != null && !roomLabel.isBlank() && bedLabel != null && !bedLabel.isBlank()) {
            return new BedAssignment(roomLabel.strip(), bedLabel.strip());
        }
        BedSuggestionSummary suggestion = suggestBed(hospitalServiceId, authorizationHeader);
        if (!suggestion.available() || suggestion.room() == null || suggestion.bed() == null) {
            String msg = suggestion.message() != null && !suggestion.message().isBlank()
                    ? suggestion.message()
                    : "Aucun lit libre pour ce service";
            throw new ConflictException(msg);
        }
        return new BedAssignment(suggestion.room(), suggestion.bed());
    }

    public BedSuggestionSummary suggestBed(Long hospitalServiceId, String authorizationHeader) {
        try {
            BedSuggestionSummary summary = restClient.get()
                    .uri("/api/v1/hospital-services/{id}/bed-suggestion", hospitalServiceId)
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(BedSuggestionSummary.class);
            if (summary == null) {
                throw new ConflictException("Impossible de proposer un lit pour ce service");
            }
            return summary;
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Service hospitalier introuvable : " + hospitalServiceId);
        }
    }

    public void updateBedOccupancy(
            Long hospitalServiceId,
            String roomLabel,
            String bedLabel,
            boolean occupied,
            Long patientId,
            Long admissionId,
            String authorizationHeader
    ) {
        if (roomLabel == null || roomLabel.isBlank() || bedLabel == null || bedLabel.isBlank()) {
            return;
        }
        try {
            restClient.patch()
                    .uri("/api/v1/hospital-services/{serviceId}/beds/occupancy", hospitalServiceId)
                    .header("Authorization", authorizationHeader)
                    .body(new BedOccupancyPayload(
                            roomLabel.strip(),
                            bedLabel.strip(),
                            occupied,
                            patientId,
                            admissionId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Lit non trouvé pour occupation {}-{}", roomLabel, bedLabel);
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Conflit occupation lit {}-{} : {}", roomLabel, bedLabel, e.getMessage());
        }
    }

    public record BedAssignment(String roomLabel, String bedLabel) {
    }

    private record BedOccupancyPayload(
            String roomLabel,
            String bedLabel,
            boolean occupied,
            Long patientId,
            Long admissionId
    ) {
    }
}
