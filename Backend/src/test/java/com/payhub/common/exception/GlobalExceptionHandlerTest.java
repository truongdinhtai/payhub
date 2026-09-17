package com.payhub.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Verifies every branch of {@link GlobalExceptionHandler} produces the correct
 * HTTP status and RFC 7807 problem body. Uses a standalone MockMvc wired to a
 * throwaway controller, so no Spring context or database is needed.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFound_returns404Problem() throws Exception {
        mockMvc.perform(get("/boom/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value("Subscription not found: 42"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void forbidden_returns403Problem() throws Exception {
        mockMvc.perform(get("/boom/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void businessRule_returns409Problem() throws Exception {
        mockMvc.perform(get("/boom/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Business rule violation"));
    }

    @Test
    void unexpected_returns500Problem() throws Exception {
        mockMvc.perform(get("/boom/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"));
    }

    @Test
    void validationFailure_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/boom/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @RestController
    @RequestMapping("/boom")
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw ResourceNotFoundException.of("Subscription", 42);
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenResourceException("Not your resource");
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessRuleException("Cannot downgrade active plan");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("kaboom");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody Payload payload) {
            // never reached with invalid payload
        }

        record Payload(@NotBlank String name) {
        }
    }
}
