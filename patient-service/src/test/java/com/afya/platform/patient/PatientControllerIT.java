package com.afya.platform.patient;

import com.afya.platform.patient.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAndSearchPatient() throws Exception {
        String body = """
                {
                  "firstName": "Marie",
                  "lastName": "Kabila",
                  "birthDate": "%s",
                  "sex": "F",
                  "phone": "+243900000001"
                }
                """.formatted(LocalDate.of(1990, 5, 12));

        mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dossierNumber").isNotEmpty());

        mockMvc.perform(get("/api/v1/patients")
                        .param("query", "Kabila")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Kabila"));
    }

    @Test
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"birthDate\":\"1990-01-01\",\"sex\":\"M\"}"))
                .andExpect(status().isUnauthorized());
    }
}
