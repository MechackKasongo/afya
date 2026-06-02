package com.afya.platform.shared.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HttpResilienceInterceptorTest {

    @Test
    void shouldRetryGetAfterIOException() throws Exception {
        HttpResilienceInterceptor interceptor = new HttpResilienceInterceptor(
                2, Duration.ZERO, 5, Duration.ofSeconds(30));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.local/a"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse ok = mock(ClientHttpResponse.class);
        when(ok.getStatusCode()).thenReturn(HttpStatus.OK);
        when(execution.execute(any(), any())).thenThrow(new IOException("boom")).thenReturn(ok);

        interceptor.intercept(request, new byte[0], execution);

        verify(execution, times(2)).execute(any(), any());
    }

    @Test
    void shouldNotRetryPostAfterIOException() throws Exception {
        HttpResilienceInterceptor interceptor = new HttpResilienceInterceptor(
                3, Duration.ZERO, 5, Duration.ofSeconds(30));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.local/a"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(IOException.class);

        verify(execution, times(1)).execute(any(), any());
    }

    @Test
    void shouldOpenCircuitAfterConsecutiveFailures() throws Exception {
        HttpResilienceInterceptor interceptor = new HttpResilienceInterceptor(
                1, Duration.ZERO, 2, Duration.ofMinutes(1));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.local/a"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> interceptor.intercept(request, new byte[0], execution))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Circuit ouvert");

        verify(execution, times(2)).execute(any(), any());
    }
}
