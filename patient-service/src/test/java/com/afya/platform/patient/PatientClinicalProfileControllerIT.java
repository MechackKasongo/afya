package com.afya.platform.patient;

import com.afya.platform.patient.support.TestJwtFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PatientClinicalProfileControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void manageMedicalAntecedentsAndEmergencyContacts() throws Exception {
        String patientBody = """
                {
                  "firstName": "Paul",
                  "lastName": "Mukendi",
                  "birthDate": "%s",
                  "sex": "M"
                }
                """.formatted(LocalDate.of(1985, 3, 20));

        String patientJson = mockMvc.perform(post("/api/v1/patients")
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patientBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode patientNode = objectMapper.readTree(patientJson);
        long patientId = patientNode.get("id").asLong();

        String antecedentBody = """
                {
                  "type": "ALLERGIE",
                  "description": "Allergie à la pénicilline",
                  "eventDate": "2010-06-15"
                }
                """;

        String antecedentJson = mockMvc.perform(post("/api/v1/patients/{id}/medical-antecedents", patientId)
                        .header("Authorization", "Bearer " + TestJwtFactory.doctorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(antecedentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ALLERGIE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode antecedentNode = objectMapper.readTree(antecedentJson);
        long antecedentId = antecedentNode.get("id").asLong();

        mockMvc.perform(get("/api/v1/patients/{id}/medical-antecedents", patientId)
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Allergie à la pénicilline"));

        mockMvc.perform(put("/api/v1/patients/{id}/medical-antecedents/{antecedentId}", patientId, antecedentId)
                        .header("Authorization", "Bearer " + TestJwtFactory.doctorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "MEDICAL",
                                  "description": "Hypertension artérielle",
                                  "eventDate": "2015-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MEDICAL"));

        String contactBody = """
                {
                  "firstName": "Grace",
                  "lastName": "Mukendi",
                  "relationship": "Épouse",
                  "phone": "+243900000099"
                }
                """;

        String contactJson = mockMvc.perform(post("/api/v1/patients/{id}/emergency-contacts", patientId)
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contactBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phone").value("+243900000099"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode contactNode = objectMapper.readTree(contactJson);
        long contactId = contactNode.get("id").asLong();

        mockMvc.perform(get("/api/v1/patients/{id}/emergency-contacts", patientId)
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Grace"));

        mockMvc.perform(delete("/api/v1/patients/{id}/medical-antecedents/{antecedentId}", patientId, antecedentId)
                        .header("Authorization", "Bearer " + TestJwtFactory.doctorToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/patients/{id}/emergency-contacts/{contactId}", patientId, contactId)
                        .header("Authorization", "Bearer " + TestJwtFactory.receptionToken()))
                .andExpect(status().isNoContent());
    }
}
