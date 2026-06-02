package com.afya.platform.shared.web;

import java.time.Instant;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        String traceId
) {
    public ApiErrorResponse(int status, String error, String message, Instant timestamp) {
        this(status, error, message, timestamp, null);
    }
}
