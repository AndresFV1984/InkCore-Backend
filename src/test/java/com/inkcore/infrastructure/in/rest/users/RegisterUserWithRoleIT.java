package com.inkcore.infrastructure.in.rest.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduce el payload de Postman: POST /register con campo {@code "role":"ADMINISTRADOR"}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RegisterUserWithRoleIT {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void register_withRoleField_createsUserRoles() throws Exception {
        String loginJson = """
                {"mail":"admin@indicolors.com","password":"Indicore2026!"}
                """;
        String loginResponse = mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = loginResponse.replaceAll("(?s).*\"token\"\\s*:\\s*\"Bearer\\s+([^\"]+)\".*", "$1");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String body = """
                {
                  "companyId": "company-seed-001",
                  "identificationNumber": "REG%s",
                  "documentType": "CC",
                  "name": "Bayron Test",
                  "mail": "bayron.role.%s@indicolors.com",
                  "contact": "111111111",
                  "department": "Antioquia",
                  "city": "Medellín",
                  "address": "Calle 10 # 20-30",
                  "password": "SecurePass123*",
                  "role": "ADMINISTRADOR",
                  "state": true
                }
                """.formatted(suffix, suffix);

        String response = mockMvc.perform(post("/api/v1/users/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.roles[0].role").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.data.roles[0].permissions").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String userId = response.replaceAll("(?s).*\"userId\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        Integer links = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM indicolors.user_roles WHERE user_id = ?",
                Integer.class,
                userId
        );
        assertEquals(1, links);

        jdbcTemplate.update("DELETE FROM indicolors.user_roles WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM indicolors.users WHERE user_id = ?", userId);
    }
}
