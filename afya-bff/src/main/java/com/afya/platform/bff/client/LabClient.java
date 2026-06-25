package com.afya.platform.bff.client;

import com.afya.platform.bff.config.DownstreamRestClientFactory;
import com.afya.platform.bff.dto.lab.*;
import com.afya.platform.bff.support.AuthorizationSupport;
import com.afya.platform.bff.support.PageRestSupport;
import com.afya.platform.bff.support.RestClientTypes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class LabClient {

    private final RestClient restClient;

    public LabClient(
            @Value("${app.services.lab-base-url}") String baseUrl,
            DownstreamRestClientFactory restClientFactory
    ) {
        this.restClient = restClientFactory.create(baseUrl);
    }

    public List<ExamTypeResponse> listExamTypes(String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/lab/exam-types")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(RestClientTypes.list(ExamTypeResponse.class));
    }

    public ExamTypeResponse createExamType(ExamTypeRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/lab/exam-types")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ExamTypeResponse.class);
    }

    public ExamRequestResponse createRequest(ExamRequestCreateRequest request, String authorizationHeader) {
        return restClient.post()
                .uri("/api/v1/lab/exam-requests")
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ExamRequestResponse.class);
    }

    public Page<ExamRequestResponse> listRequests(
            ExamRequestStatus status,
            Integer page,
            Integer size,
            String authorizationHeader
    ) {
        return PageRestSupport.getPage(
                restClient,
                uriBuilder -> uriBuilder.path("/api/v1/lab/exam-requests")
                        .queryParamIfPresent("status", Optional.ofNullable(status))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("size", Optional.ofNullable(size))
                        .build(),
                ExamRequestResponse.class,
                headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)));
    }

    public ExamRequestResponse getRequest(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/lab/exam-requests/{id}", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(ExamRequestResponse.class);
    }

    public ExamRequestResponse recordSpecimen(
            Long id,
            SpecimenCollectionRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/lab/exam-requests/{id}/specimen", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ExamRequestResponse.class);
    }

    public ExamResultResponse recordResult(
            Long id,
            ExamResultRequest request,
            String authorizationHeader
    ) {
        return restClient.post()
                .uri("/api/v1/lab/exam-requests/{id}/result", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .body(request)
                .retrieve()
                .body(ExamResultResponse.class);
    }

    public ExamResultResponse getResult(Long id, String authorizationHeader) {
        return restClient.get()
                .uri("/api/v1/lab/exam-requests/{id}/result", id)
                .headers(headers -> headers.addAll(AuthorizationSupport.bearerHeaders(authorizationHeader)))
                .retrieve()
                .body(ExamResultResponse.class);
    }
}
