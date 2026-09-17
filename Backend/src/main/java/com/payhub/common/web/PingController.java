package com.payhub.common.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness endpoint that also serves as a smoke test for the
 * API versioning scheme, OpenAPI wiring and CORS configuration.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "System", description = "Health and metadata endpoints")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "Liveness check", description = "Returns a static status payload.")
    public PingResponse ping() {
        return new PingResponse("ok", "v1", Instant.now());
    }

    public record PingResponse(String status, String apiVersion, Instant timestamp) {
    }
}
