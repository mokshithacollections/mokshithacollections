package com.ec.mokshitha_collections.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for the auth flow. Demonstrates the intended pattern — extend
 * with cart/order/admin tests as the suite grows.
 *
 * Uses an H2 in-memory DB in PostgreSQL-compat mode (see
 * src/test/resources/application-test.properties). The admin seeder runs
 * automatically too, so register-paths must use a different email.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void register_then_login_succeeds() throws Exception {
        mvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "alice@example.com")
                        .param("password", "Password1")
                        .param("firstName", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        mvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "alice@example.com")
                        .param("password", "Password1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"));
    }

    @Test
    void login_with_wrong_password_returns_401() throws Exception {
        mvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "bob@example.com")
                        .param("password", "Password1")
                        .param("firstName", "Bob"))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "bob@example.com")
                        .param("password", "WrongPass1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_rejects_duplicate_email() throws Exception {
        mvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "carol@example.com")
                        .param("password", "Password1")
                        .param("firstName", "Carol"))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "carol@example.com")
                        .param("password", "Password1")
                        .param("firstName", "Carol2"))
                .andExpect(status().isConflict());
    }

    @Test
    void login_rate_limit_returns_429_after_threshold() throws Exception {
        mvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "dan@example.com")
                        .param("password", "Password1")
                        .param("firstName", "Dan"))
                .andExpect(status().isOk());

        // 5 failed attempts get the IP blocked; the 6th must be 429.
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("email", "dan@example.com")
                            .param("password", "WrongPass" + i))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "dan@example.com")
                        .param("password", "Password1")) // even the right password is locked out
                .andExpect(status().isTooManyRequests());
    }
}
