package com.afya.platform.shared.http;

import com.afya.platform.shared.web.CorrelationIdSupport;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

public final class RestClients {

    private RestClients() {
    }

    public static RestClient create(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        return create(baseUrl, connectTimeout, readTimeout, 2, Duration.ofMillis(250), 5, Duration.ofSeconds(30));
    }

    public static RestClient create(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            int maxAttempts,
            Duration retryDelay,
            int failureThreshold,
            Duration openDuration
    ) {
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
                .requestInterceptor(new HttpResilienceInterceptor(maxAttempts, retryDelay, failureThreshold, openDuration))
                .build();
    }
}
