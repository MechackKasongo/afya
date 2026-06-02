package com.afya.platform.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldReuseIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdSupport.HEADER, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                assertThat(req.getAttribute(CorrelationIdSupport.REQUEST_ATTRIBUTE)).isEqualTo("corr-123");
                assertThat(MDC.get(CorrelationIdSupport.MDC_KEY)).isEqualTo("corr-123");
            }
        });

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdSupport.HEADER)).isEqualTo("corr-123");
        assertThat(MDC.get(CorrelationIdSupport.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(CorrelationIdSupport.HEADER);
        assertThat(generated).isNotBlank();
        assertThat(request.getAttribute(CorrelationIdSupport.REQUEST_ATTRIBUTE)).isEqualTo(generated);
    }
}
