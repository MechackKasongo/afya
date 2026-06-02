package com.afya.platform.shared.http;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight retry + circuit breaker for downstream calls.
 * Retries are intentionally limited to idempotent methods.
 */
public class HttpResilienceInterceptor implements ClientHttpRequestInterceptor {

    private final int maxAttempts;
    private final Duration retryDelay;
    private final int failureThreshold;
    private final Duration openDuration;
    private final Map<String, CircuitState> circuitByHost = new ConcurrentHashMap<>();

    public HttpResilienceInterceptor(int maxAttempts, Duration retryDelay, int failureThreshold, Duration openDuration) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryDelay = retryDelay == null ? Duration.ofMillis(250) : retryDelay;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDuration = openDuration == null ? Duration.ofSeconds(30) : openDuration;
    }

    @Override
    public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        String key = hostKey(request.getURI());
        CircuitState state = circuitByHost.computeIfAbsent(key, ignored -> new CircuitState());
        ensureCircuitClosed(key, state);

        boolean retryableMethod = isRetryableMethod(request.getMethod());
        IOException lastIo = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                HttpStatusCode status = response.getStatusCode();
                if (status.is5xxServerError()) {
                    incrementCounter("downstream_5xx_total", key);
                    registerFailure(state, key);
                    if (retryableMethod && attempt < maxAttempts) {
                        incrementCounter("retry_total", key);
                        response.close();
                        pause();
                        continue;
                    }
                    return response;
                }
                registerSuccess(state);
                return response;
            } catch (IOException io) {
                incrementCounter("downstream_io_failure_total", key);
                registerFailure(state, key);
                lastIo = io;
                if (!retryableMethod || attempt >= maxAttempts) {
                    throw io;
                }
                incrementCounter("retry_total", key);
                pause();
            }
        }

        if (lastIo != null) {
            throw lastIo;
        }
        throw new ResourceAccessException("Erreur de communication en aval");
    }

    private void ensureCircuitClosed(String key, CircuitState state) {
        Instant now = Instant.now();
        Instant openedUntil = state.openUntil;
        if (openedUntil != null && now.isBefore(openedUntil)) {
            incrementCounter("circuit_reject_total", key);
            throw new ResourceAccessException("Circuit ouvert pour " + key + " jusqu'à " + openedUntil);
        }
        if (openedUntil != null && !now.isBefore(openedUntil)) {
            state.openUntil = null;
            state.failures.set(0);
        }
    }

    private void registerFailure(CircuitState state, String key) {
        int failures = state.failures.incrementAndGet();
        Instant openedUntil = state.openUntil;
        if (failures >= failureThreshold && openedUntil == null) {
            state.openUntil = Instant.now().plus(openDuration);
            incrementCounter("circuit_open_total", key);
        }
    }

    private void registerSuccess(CircuitState state) {
        state.failures.set(0);
        state.openUntil = null;
    }

    private boolean isRetryableMethod(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS;
    }

    private void pause() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private String hostKey(URI uri) {
        int port = uri.getPort() >= 0 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
        return uri.getScheme() + "://" + uri.getHost() + ":" + port;
    }

    private void incrementCounter(String metricSuffix, String target) {
        Counter.builder("afya.http.resilience." + metricSuffix)
                .tag("target", target)
                .register(Metrics.globalRegistry)
                .increment();
    }

    private static final class CircuitState {
        private final AtomicInteger failures = new AtomicInteger(0);
        private volatile Instant openUntil;
    }
}
