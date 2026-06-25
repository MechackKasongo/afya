package com.afya.platform.bff.controller;

import com.afya.platform.bff.client.MedicalClient;
import com.afya.platform.bff.dto.DiseaseCatalogResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DiseaseCatalogBffMappingIT {

    private static final String SECRET =
            "dev-access-secret-at-least-64-characters-long-for-hs512-signing-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalClient medicalClient;

    @Test
    void diseaseCatalogRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/disease-catalog").param("diseaseType", "Chronique"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void diseaseCatalogProxiesClinicalRecordWhenAuthenticated() throws Exception {
        when(medicalClient.listSelectableDiseases(eq("Chronique"), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(new DiseaseCatalogResponse(1L, "Chronique", "Diabète type 2", 5, true)));

        mockMvc.perform(get("/api/v1/disease-catalog")
                        .param("diseaseType", "Chronique")
                        .header("Authorization", "Bearer " + medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Diabète type 2"))
                .andExpect(jsonPath("$[0].usageCount").value(5))
                .andExpect(jsonPath("$[0].selectable").value(true));
    }

    private static String medecinToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject("medecin")
                .claim("roles", List.of("MEDECIN"))
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
