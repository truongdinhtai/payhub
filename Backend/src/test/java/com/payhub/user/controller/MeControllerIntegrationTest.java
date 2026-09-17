package com.payhub.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * Verifies {@code GET /api/v1/me} resolves the caller from the JWT subject and
 * that the endpoint is protected (401 without a token).
 */
@SpringBootTest
@AutoConfigureMockMvc
class MeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void me_returnsCurrentUser_whenAuthenticated() throws Exception {
        User user = userService.upsertFromGoogle("sub-me", "me@example.com", "Me User", "pic");

        mockMvc.perform(get("/api/v1/me")
                        .with(jwt().jwt(builder -> builder.subject(user.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void me_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patchMe_updatesName() throws Exception {
        User user = userService.upsertFromGoogle("sub-edit", "edit@example.com", "Old Name", null);

        mockMvc.perform(patch("/api/v1/me")
                        .with(jwt().jwt(builder -> builder.subject(user.getId().toString())))
                        .contentType("application/json")
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }
}
