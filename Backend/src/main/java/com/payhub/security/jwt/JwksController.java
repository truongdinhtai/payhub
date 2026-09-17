package com.payhub.security.jwt;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the RSA public key as a JWK Set so clients can verify PayHub tokens
 * without sharing a secret. Only public key material is exposed.
 */
@RestController
@Tag(name = "System", description = "Health and metadata endpoints")
public class JwksController {

    private final RsaKeyProvider keys;

    public JwksController(RsaKeyProvider keys) {
        this.keys = keys;
    }

    @GetMapping("/oauth2/jwks")
    @Operation(summary = "JSON Web Key Set", description = "Public keys used to verify PayHub JWTs.")
    public Map<String, Object> jwks() {
        return keys.publicJwkSet().toJSONObject();
    }
}
