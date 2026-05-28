package com.afya.platform.clinical;

import com.afya.platform.clinical.integration.PatientServiceClient;
import com.afya.platform.clinical.integration.PatientSummary;
import com.afya.platform.clinical.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClinicalDocumentUploadIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientServiceClient patientServiceClient;

    @Test
    void uploadAndDownloadDocument() throws Exception {
        when(patientServiceClient.getPatient(anyLong(), anyString()))
                .thenReturn(new PatientSummary(1L, "Jean", "Mukendi", "DOS-2026-AAAA-0001"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "radio.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "contenu-test".getBytes());

        String uploadJson = mockMvc.perform(multipart("/api/v1/patients/1/documents/upload")
                        .file(file)
                        .param("title", "Radio thorax")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Radio thorax"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long documentId = Long.parseLong(uploadJson.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(get("/api/v1/patients/1/documents/" + documentId + "/download")
                        .header("Authorization", "Bearer " + TestJwtFactory.medecinToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Radio thorax")));
    }
}
