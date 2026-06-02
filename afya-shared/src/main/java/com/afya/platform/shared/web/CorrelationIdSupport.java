package com.afya.platform.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

public final class CorrelationIdSupport {

    public static final String HEADER = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = "correlationId";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIdSupport() {
    }

    public static String currentId() {
        String fromMdc = MDC.get(MDC_KEY);
        if (fromMdc != null && !fromMdc.isBlank()) {
            return fromMdc;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            Object attribute = request.getAttribute(REQUEST_ATTRIBUTE);
            if (attribute instanceof String correlationId && !correlationId.isBlank()) {
                return correlationId;
            }
            String header = request.getHeader(HEADER);
            if (header != null && !header.isBlank()) {
                return header;
            }
        }
        return UUID.randomUUID().toString();
    }

    public static Optional<String> currentIdOptional() {
        String id = currentId();
        return id == null || id.isBlank() ? Optional.empty() : Optional.of(id);
    }
}
