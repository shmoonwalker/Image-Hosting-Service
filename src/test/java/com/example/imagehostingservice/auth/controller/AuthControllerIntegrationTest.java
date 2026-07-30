package com.example.imagehostingservice.auth.controller;

import com.example.imagehostingservice.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM spring_session");
        jdbcTemplate.update("DELETE FROM images");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void registerShouldCreateUserAndAuthenticatedSession() throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Shadi",
                                          "email": "shadi@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Shadi"))
                .andExpect(jsonPath("$.email").value("shadi@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        Cookie sessionCookie =
                result.getResponse().getCookie("SESSION");

        assertNotNull(sessionCookie);

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .cookie(sessionCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Shadi"))
                .andExpect(jsonPath("$.email").value("shadi@example.com"));
    }

    @Test
    void loginAndLogoutShouldManageSession() throws Exception {
        userRepository.save(
                "Shadi",
                "shadi@example.com",
                passwordEncoder.encode("password123")
        );

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "shadi@example.com",
                                          "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email")
                        .value("shadi@example.com"))
                .andReturn();

        Cookie sessionCookie =
                loginResult.getResponse().getCookie("SESSION");

        assertNotNull(sessionCookie);

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .cookie(sessionCookie)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/auth/logout")
                                .cookie(sessionCookie)
                                .with(csrf())
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/auth/me")
                                .cookie(sessionCookie)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"));
    }

    @Test
    void duplicateRegistrationShouldReturnConflict() throws Exception {
        String requestBody = """
                {
                  "name": "Shadi",
                  "email": "shadi@example.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Email is already in use"));
    }

    @Test
    void unauthenticatedMeShouldReturnJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentication is required"))
                .andExpect(jsonPath("$.path")
                        .value("/api/v1/auth/me"));
    }
}