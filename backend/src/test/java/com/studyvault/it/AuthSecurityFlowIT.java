package com.studyvault.it;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end security flow against a real PostgreSQL (Testcontainers).
 * Skips automatically when Docker is unavailable (local development uses
 * the scripted E2E against a live server instead - see docs/TESTING.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthSecurityFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.email.provider", () -> "log");
        registry.add("app.storage.provider", () -> "local");
        registry.add("app.storage.local-dir", () -> "./target/test-storage");
        registry.add("app.payments.provider", () -> "mock");
    }

    @Autowired
    private MockMvc mvc;

    private static String csrfToken;
    private static Cookie sessionCookie;

    @Test
    @Order(1)
    void csrfEndpointIssuesTokenAndCookie() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        csrfToken = extract("$.data.token", result.getResponse().getContentAsString());
    }

    @Test
    @Order(2)
    void mutatingRequestWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"fullName\":\"A B\",\"email\":\"a@b.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void registerLoginMeLogoutRoundTrip() throws Exception {
        // register
        mvc.perform(post("/api/v1/auth/register")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"fullName\":\"Test User\",\"email\":\"e2e@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isCreated());

        // login → session cookie
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"email\":\"e2e@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andReturn();
        sessionCookie = login.getResponse().getCookie("sv_session");
        org.assertj.core.api.Assertions.assertThat(sessionCookie).isNotNull();
        org.assertj.core.api.Assertions.assertThat(sessionCookie.isHttpOnly()).isTrue();

        // me
        mvc.perform(get("/api/v1/auth/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("e2e@example.com"));

        // logout
        mvc.perform(post("/api/v1/auth/logout").header("X-XSRF-TOKEN", csrfToken).cookie(sessionCookie))
                .andExpect(status().isOk());

        // me after logout → 401
        mvc.perform(get("/api/v1/auth/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    void anonymousGets401AndUserGets403OnAdminEndpoints() throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());

        // fresh login
        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"email\":\"e2e@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        sessionCookie = login.getResponse().getCookie("sv_session");

        mvc.perform(get("/api/v1/admin/dashboard").cookie(sessionCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void weakPasswordsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"fullName\":\"W P\",\"email\":\"weak@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    void wrongPasswordIsGenericAndNeverLocksOutImmediately() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"email\":\"e2e@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    private String extract(String jsonPathFragment, String body) {
        Matcher matcher = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        org.assertj.core.api.Assertions.assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
