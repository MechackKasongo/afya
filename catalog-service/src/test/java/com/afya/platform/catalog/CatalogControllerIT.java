package com.afya.platform.catalog;

import com.afya.platform.catalog.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
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
class CatalogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listHospitalServicesWithJwt() throws Exception {
        mockMvc.perform(get("/api/v1/hospital-services")
                        .header("Authorization", "Bearer " + TestJwtFactory.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    void createDepartmentAsAdmin() throws Exception {
        String body = """
                {"code":"PEDIA","name":"Pédiatrie","active":true}
                """;
        mockMvc.perform(post("/api/v1/departments")
                        .header("Authorization", "Bearer " + TestJwtFactory.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("PEDIA"));
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized());
    }
}
