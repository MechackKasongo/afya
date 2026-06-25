package com.afya.platform.nursing;

import com.afya.platform.nursing.integration.AdmissionSummary;
import com.afya.platform.nursing.integration.CareEntryServiceClient;
import com.afya.platform.nursing.integration.PatientServiceClient;
import com.afya.platform.nursing.integration.PatientSummary;
import com.afya.platform.nursing.support.TestJwtFactory;
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
class ConsultationControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @MockitoBean
    private CareEntryServiceClient careEntryServiceClient;

    @Test
    void consultationLifecycle() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));
        when(careEntryServiceClient.getAdmission(anyLong(), anyString()))
                .thenReturn(new AdmissionSummary(10L, 1L));

        String createBody = """
                {
                  "patientId": 1,
                  "admissionId": 10,
                  "doctorName": "Dr Test",
                  "reason": "Suivi post-op"
                }
                """;

        String consultationJson = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(1))
                .andExpect(jsonPath("$.admissionId").value(10))
                .andExpect(jsonPath("$.doctorName").value("Dr Test"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long consultationId = Long.parseLong(consultationJson.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/observations")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Patient stable\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("OBSERVATION"));

        mockMvc.perform(get("/api/v1/consultations/" + consultationId + "/events")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Patient stable"));

        mockMvc.perform(get("/api/v1/patients/1/consultation-events")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/consultations/patient-timeline").param("patientId", "1")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/consultations?patientId=1")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(consultationId));
    }

    @Test
    void diagnosticWithDiseaseCatalogAfterFiveUsages() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));
        when(careEntryServiceClient.getAdmission(anyLong(), anyString()))
                .thenReturn(new AdmissionSummary(10L, 1L));

        String consultationJson = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 1,
                                  "admissionId": 10,
                                  "doctorName": "Dr Test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long consultationId = Long.parseLong(consultationJson.split("\"id\":")[1].split(",")[0]);

        String diagnosticBody = """
                {
                  "diseaseType": "Infectieuse",
                  "diseaseName": "Paludisme IT",
                  "content": "Suspicion paludisme"
                }
                """;

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/diagnostics")
                            .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(diagnosticBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.type").value("DIAGNOSTIC"))
                    .andExpect(jsonPath("$.diseaseType").value("Infectieuse"))
                    .andExpect(jsonPath("$.diseaseName").value("Paludisme IT"));
        }

        mockMvc.perform(get("/api/v1/disease-catalog")
                        .param("diseaseType", "Infectieuse")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/diagnostics")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(diagnosticBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/disease-catalog")
                        .param("diseaseType", "Infectieuse")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].label").value("Paludisme IT"))
                .andExpect(jsonPath("$[0].usageCount").value(5))
                .andExpect(jsonPath("$[0].selectable").value(true));
    }

    @Test
    void diagnosticRequiresDiseaseTypeAndName() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));
        when(careEntryServiceClient.getAdmission(anyLong(), anyString()))
                .thenReturn(new AdmissionSummary(10L, 1L));

        String consultationJson = mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 1,
                                  "admissionId": 10,
                                  "doctorName": "Dr Test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long consultationId = Long.parseLong(consultationJson.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post("/api/v1/consultations/" + consultationId + "/diagnostics")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Sans maladie\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAdmissionPatientMismatch() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));
        when(careEntryServiceClient.getAdmission(anyLong(), anyString()))
                .thenReturn(new AdmissionSummary(10L, 99L));

        mockMvc.perform(post("/api/v1/consultations")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "patientId": 1,
                                  "admissionId": 10,
                                  "doctorName": "Dr Test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
