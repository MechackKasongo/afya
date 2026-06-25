package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.BedResponse;
import com.afya.platform.bff.dto.BedSuggestionResponse;
import com.afya.platform.bff.dto.CatalogOccupancyStatsResponse;
import com.afya.platform.bff.dto.DepartmentRequest;
import com.afya.platform.bff.dto.DepartmentResponse;
import com.afya.platform.bff.dto.HospitalServiceRequest;
import com.afya.platform.bff.dto.HospitalServiceResponse;
import com.afya.platform.bff.dto.HospitalServiceStatusRequest;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class HospitalClient {

    private final RestClient restClient;

    public HospitalClient(
            @Value("${app.services.hospital-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public Page<HospitalServiceResponse> listHospitalServices(
            Boolean activeOnly,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder
                        .path("/api/v1/hospital-services")
                        .queryParamIfPresent("activeOnly", java.util.Optional.ofNullable(activeOnly))
                        .queryParamIfPresent("page", java.util.Optional.ofNullable(page))
                        .queryParamIfPresent("size", java.util.Optional.ofNullable(size))
                        .build(),
                HospitalServiceResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public HospitalServiceResponse getById(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/hospital-services/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(HospitalServiceResponse.class);
    }

    public HospitalServiceResponse create(HospitalServiceRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/hospital-services")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(HospitalServiceResponse.class);
    }

    public HospitalServiceResponse update(Long id, HospitalServiceRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/hospital-services/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(HospitalServiceResponse.class);
    }

    public HospitalServiceResponse updateStatus(
            Long id,
            HospitalServiceStatusRequest request,
            String authorizationHeader
    ) {
        return restClient.patch()
                .uri("/api/v1/hospital-services/{id}/status", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(HospitalServiceResponse.class);
    }

    public void delete(Long id, String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/hospital-services/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }

    public CatalogOccupancyStatsResponse occupancyStats(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/stats/occupancy")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(CatalogOccupancyStatsResponse.class);
    }

    public List<BedResponse> listBeds(Long hospitalServiceId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/hospital-services/{serviceId}/beds", hospitalServiceId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public int provisionBeds(Long hospitalServiceId, String authorizationHeader) {
        Integer created = restClient.post()
                .uri("/api/v1/hospital-services/{serviceId}/beds/provision", hospitalServiceId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(Integer.class);
        return created == null ? 0 : created;
    }

    public int realignBeds(Long hospitalServiceId, String authorizationHeader) {
        Integer created = restClient.post()
                .uri("/api/v1/hospital-services/{serviceId}/beds/realign", hospitalServiceId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(Integer.class);
        return created == null ? 0 : created;
    }

    public BedSuggestionResponse bedSuggestion(Long serviceId, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/hospital-services/{serviceId}/bed-suggestion", serviceId)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(BedSuggestionResponse.class);
    }

    public List<DepartmentResponse> listDepartments(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/departments")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public DepartmentResponse getDepartment(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/departments/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(DepartmentResponse.class);
    }

    public DepartmentResponse createDepartment(DepartmentRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/departments")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(DepartmentResponse.class);
    }

    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request, String authorizationHeader) {
        return restClient.put()
                .uri("/api/v1/departments/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(DepartmentResponse.class);
    }

    public void deleteDepartment(Long id, String authorizationHeader) {
        restClient.delete()
                .uri("/api/v1/departments/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .toBodilessEntity();
    }
}
