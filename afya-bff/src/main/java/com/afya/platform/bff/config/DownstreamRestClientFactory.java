package com.afya.platform.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.afya.platform.shared.http.HttpResilienceInterceptor;
import com.afya.platform.shared.web.CorrelationIdSupport;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class DownstreamRestClientFactory {

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int retryMaxAttempts;
    private final Duration retryDelay;
    private final int circuitFailureThreshold;
    private final Duration circuitOpenDuration;

    public DownstreamRestClientFactory(
            @Value("${app.http.client.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.http.client.read-timeout:10s}") Duration readTimeout,
            @Value("${app.http.client.retry.max-attempts:2}") int retryMaxAttempts,
            @Value("${app.http.client.retry.delay:250ms}") Duration retryDelay,
            @Value("${app.http.client.circuit.failure-threshold:5}") int circuitFailureThreshold,
            @Value("${app.http.client.circuit.open-duration:30s}") Duration circuitOpenDuration
    ) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelay = retryDelay;
        this.circuitFailureThreshold = circuitFailureThreshold;
        this.circuitOpenDuration = circuitOpenDuration;
    }

    public RestClient create(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    CorrelationIdSupport.currentIdOptional().ifPresent(id ->
                            request.getHeaders().set(CorrelationIdSupport.HEADER, id));
                    return execution.execute(request, body);
                })
                .requestInterceptor(new HttpResilienceInterceptor(
                        retryMaxAttempts,
                        retryDelay,
                        circuitFailureThreshold,
                        circuitOpenDuration
                ))
                .build();
    }
}
