package com.afya.platform.clinical;

import com.afya.platform.clinical.integration.PatientServiceClient;
import com.afya.platform.clinical.integration.PatientSummary;
import com.afya.platform.clinical.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClinicalRecordControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @Test
    void prescriptionAndAdministration() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));

        String rxBody = """
                {
                  "drugName": "Paracétamol",
                  "prescriptionDetails": "500 mg, 3 fois par jour"
                }
                """;

        String rxJson = mockMvc.perform(post("/api/v1/patients/1/prescriptions")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rxBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drugName").value("Paracétamol"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long lineId = Long.parseLong(rxJson.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post("/api/v1/prescriptions/" + lineId + "/administrations")
                        .header("Authorization", "Bearer " + TestJwtFactory.infirmierToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prescriptionLineId").value(lineId));

        mockMvc.perform(get("/api/v1/patients/1/medical-record")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptions[0].administered").value(true));
    }
}
