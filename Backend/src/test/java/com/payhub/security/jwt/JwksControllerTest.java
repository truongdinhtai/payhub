package com.payhub.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Ensures the JWKS endpoint exposes the public key and never leaks private key
 * material (the RSA private exponent {@code d}).
 */
class JwksControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void jwks_exposesPublicKeyOnly() {
        JwksController controller = new JwksController(new RsaKeyProvider("", ""));

        Map<String, Object> jwks = controller.jwks();

        assertThat(jwks).containsKey("keys");
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
        assertThat(keys).hasSize(1);
        Map<String, Object> key = keys.get(0);
        assertThat(key).containsKey("n").containsKey("e"); // public modulus + exponent
        assertThat(key).doesNotContainKey("d");            // private exponent must be absent
    }
}
