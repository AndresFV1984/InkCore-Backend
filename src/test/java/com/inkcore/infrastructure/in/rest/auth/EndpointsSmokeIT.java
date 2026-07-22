package com.inkcore.infrastructure.in.rest.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EndpointsSmokeIT {

    @Autowired
    MockMvc mockMvc;

    @Test
    void allCurrentEndpointsRespond() throws Exception {
        String loginJson = """
                {"mail":"admin@indicolors.com","password":"Indicore2026!"}
                """;

        String loginResponse = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headers.token").isNotEmpty())
                .andExpect(jsonPath("$.headers.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = loginResponse.replaceAll("(?s).*\"token\"\\s*:\\s*\"Bearer\\s+([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/v1/users/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/users/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mail").value("admin@indicolors.com"));

        mockMvc.perform(get("/api/v1/users/get/seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c"));

        mockMvc.perform(get("/api/v1/roles/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/permissions/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        String unique = String.valueOf(System.currentTimeMillis());
        String registerJson = """
                {
                  "companyId": "company-seed-001",
                  "name": "Usuario Smoke",
                  "documentType": "CC",
                  "identificationNumber": "%s",
                  "mail": "smoke.%s@indicolors.com",
                  "contact": "3001112233",
                  "department": "Antioquia",
                  "city": "Medellin",
                  "address": "Calle 1",
                  "roleCode": "OPERADOR",
                  "permissionCodes": [],
                  "password": "clave123",
                  "state": true
                }
                """.formatted(unique, unique);

        String registerResponse = mockMvc.perform(post("/api/v1/users/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = registerResponse.replaceAll("(?s).*\"userId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        String updateJson = """
                {
                  "name": "Usuario Smoke Editado",
                  "documentType": "CC",
                  "identificationNumber": "%s",
                  "mail": "smoke.%s@indicolors.com",
                  "contact": "3000000000",
                  "department": "Antioquia",
                  "city": "Envigado",
                  "address": "Calle 2",
                  "roleCode": "OPERADOR",
                  "permissionCodes": [],
                  "state": true
                }
                """.formatted(unique, unique);

        mockMvc.perform(put("/api/v1/users/update/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Usuario Smoke Editado"));
    }
}
