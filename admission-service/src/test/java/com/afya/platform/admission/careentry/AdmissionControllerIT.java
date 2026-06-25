package com.afya.platform.admission.careentry;

import com.afya.platform.admission.integration.CatalogServiceClient;
import com.afya.platform.admission.integration.HospitalServiceSummary;
import com.afya.platform.admission.integration.PatientServiceClient;
import com.afya.platform.admission.integration.PatientSummary;
import com.afya.platform.admission.careentry.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdmissionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @MockitoBean
    private CatalogServiceClient catalogServiceClient;

    @Test
    void createAdmission() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));
        when(catalogServiceClient.getHospitalService(anyLong(), anyString()))
                .thenReturn(new HospitalServiceSummary(2L, "Médecine interne", 20, true));

        String body = """
                {"patientId":1,"hospitalServiceId":2}
                """;

        mockMvc.perform(post("/api/v1/admissions")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OUVERTE"))
                .andExpect(jsonPath("$.patientName").value("Jean Mukendi"));
    }
}
