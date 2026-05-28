package com.afya.platform.audit;

import com.afya.platform.audit.config.IngestionKeyAuthenticationFilter;
import com.afya.platform.audit.support.TestJwtFactory;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditEventControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void ingestWithJwtAndQueryAsAdmin() throws Exception {
        String body = """
                {
                  "action": "PATIENT_CREATED",
                  "resourceType": "PATIENT",
                  "resourceId": "42",
                  "sourceService": "patient-service"
                }
                """;

        mockMvc.perform(post("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.action").value("PATIENT_CREATED"))
                .andExpect(jsonPath("$.actorUsername").value("admin"));

        mockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + TestJwtFactory.adminToken())
                        .param("action", "PATIENT_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].resourceId").value("42"));

        mockMvc.perform(get("/api/v1/reports/activity")
                        .header("Authorization", "Bearer " + TestJwtFactory.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(1))
                .andExpect(jsonPath("$.byAction[0].key").value("PATIENT_CREATED"));
    }

    @Test
    @Order(2)
    void ingestWithInternalKeyRequiresActor() throws Exception {
        String body = """
                {
                  "actorUsername": "identity-job",
                  "action": "LOGIN_SUCCESS",
                  "resourceType": "USER",
                  "resourceId": "admin",
                  "sourceService": "identity-service"
                }
                """;

        mockMvc.perform(post("/api/v1/audit/events")
                        .header(IngestionKeyAuthenticationFilter.INGESTION_HEADER, "test-ingestion-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actorUsername").value("identity-job"));
    }

    @Test
    @Order(3)
    void nonAdminCannotQueryEvents() throws Exception {
        mockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isForbidden());
    }
}
