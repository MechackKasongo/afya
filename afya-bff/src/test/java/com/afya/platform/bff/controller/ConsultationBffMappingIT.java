package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.MedicalClient;
import com.afya.platform.bff.dto.ConsultationEventResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConsultationBffMappingIT {

    private static final String SECRET =
            "dev-access-secret-at-least-64-characters-long-for-hs512-signing-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private MedicalClient medicalClient;

    @Test
    void consultationTimelineMappingsAreRegistered() {
        var patterns = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPathPatternsCondition().getPatterns().stream())
                .map(Object::toString)
                .filter(path -> path.contains("consultation") || path.contains("timeline") || path.contains("event"))
                .sorted()
                .toList();

        assertThat(patterns)
                .as("Routes consultation / timeline / events attendues")
                .anyMatch(p -> p.contains("/api/v1/consultations/") && p.contains("events"))
                .anyMatch(p -> p.contains("patient-timeline"))
                .anyMatch(p -> p.contains("consultation-events"));
    }

    @Test
    void consultationEventsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/consultations/1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void consultationEventsReturnsTimelineWhenAuthenticated() throws Exception {
        when(medicalClient.consultationEvents(anyLong(), anyString()))
                .thenReturn(List.of(new ConsultationEventResponse(
                        1L, 1L, 6L, "OBSERVATION", "Stable", null, null, Instant.parse("2026-05-26T10:00:00Z"))));

        mockMvc.perform(get("/api/v1/consultations/1/events")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Stable"));
    }

    private static String adminToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject("admin")
                .claim("roles", List.of("ADMIN"))
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
