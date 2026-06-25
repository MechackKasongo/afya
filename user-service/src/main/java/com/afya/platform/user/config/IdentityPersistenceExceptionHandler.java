package com.afya.platform.user.config;

import com.afya.platform.shared.web.ApiErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Locale;

@RestControllerAdvice
public class IdentityPersistenceExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String root = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String lower = root == null ? "" : root.toLowerCase(Locale.ROOT);
        String message = "Donnée en conflit avec une contrainte existante.";
        if (lower.contains("email") || lower.contains("app_users_email")) {
            message = "Cette adresse email est déjà utilisée par un autre compte.";
        } else if (lower.contains("username") || lower.contains("app_users_username")) {
            message = "Cet identifiant est déjà utilisé.";
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        HttpStatus.CONFLICT.getReasonPhrase(),
                        message,
                        Instant.now()));
    }
}
