package com.afya.platform.admission.stay;

import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.model.Admission;
import com.afya.platform.admission.model.AdmissionStatus;
import com.afya.platform.admission.repository.AdmissionRepository;
import com.afya.platform.admission.stay.support.TestJwtFactory;
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

    @Autowired
    private AdmissionRepository admissionRepository;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @Test
    void openStayAndUpdateForm() throws Exception {
        Admission admission = new Admission();
        admission.setPatientId(1L);
        admission.setHospitalServiceId(2L);
        admission.setAdmittedAt(Instant.now());
        admission.setStatus(AdmissionStatus.OUVERTE);
        Admission savedAdmission = admissionRepository.save(admission);

        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));

        String openBody = """
                {"admissionId":%d,"patientId":1,"roomLabel":"A12","bedLabel":"LIT-3"}
                """.formatted(savedAdmission.getId());

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
