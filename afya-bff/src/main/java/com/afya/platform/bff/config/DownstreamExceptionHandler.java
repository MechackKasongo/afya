package com.afya.platform.bff.config;

import com.afya.platform.shared.web.ApiErrorResponse;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@RestControllerAdvice
public class DownstreamExceptionHandler {

    private final JsonMapper jsonMapper;

    public DownstreamExceptionHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstream(HttpStatusCodeException ex) {
        try {
            ApiErrorResponse body = jsonMapper.readValue(ex.getResponseBodyAsByteArray(), ApiErrorResponse.class);
            return ResponseEntity.status(ex.getStatusCode()).body(body);
        } catch (Exception ignored) {
            return ResponseEntity.status(ex.getStatusCode()).body(new ApiErrorResponse(
                    ex.getStatusCode().value(),
                    HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase(),
                    "Erreur service en aval",
                    Instant.now()
            ));
        }
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreachable(ResourceAccessException ex) {
        return serviceUnavailable(
                "Service indisponible. Vérifiez que le microservice concerné est démarré (voir docs/DEMARRAGE.md)."
        );
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiErrorResponse> handleRestClient(RestClientException ex) {
        return serviceUnavailable("Erreur de communication avec un service en aval.");
    }

    private static ResponseEntity<ApiErrorResponse> serviceUnavailable(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                message,
                Instant.now()
        ));
    }
}
