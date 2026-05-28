package com.afya.platform.stay;

import com.afya.platform.stay.integration.AdmissionSummary;
import com.afya.platform.stay.integration.CareEntryServiceClient;
import com.afya.platform.stay.integration.PatientServiceClient;
import com.afya.platform.stay.integration.PatientSummary;
import com.afya.platform.stay.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StayControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CareEntryServiceClient careEntryServiceClient;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @Test
    void openStayAndUpdateForm() throws Exception {
        when(careEntryServiceClient.getAdmission(anyLong(), anyString()))
                .thenReturn(new AdmissionSummary(10L, 1L, 2L, Instant.now(), "OUVERTE"));
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));

        String openBody = """
                {"admissionId":10,"patientId":1,"roomLabel":"A12","bedLabel":"LIT-3"}
                """;

        String stayJson = mockMvc.perform(post("/api/v1/stays")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(openBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long stayId = Long.parseLong(stayJson.split("\"id\":")[1].split(",")[0]);

        String formBody = """
                {"antecedentsText":"Aucune allergie","anamnesisText":"Fièvre depuis 2 jours","conclusionText":"Surveillance"}
                """;

        mockMvc.perform(put("/api/v1/stays/" + stayId + "/hospitalization-form")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(formBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anamnesisText").value("Fièvre depuis 2 jours"))
                .andExpect(jsonPath("$.conclusionText").value("Surveillance"));
    }
}
