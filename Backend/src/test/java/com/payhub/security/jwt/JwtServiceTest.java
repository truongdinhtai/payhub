package com.payhub.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.payhub.support.TestUsers;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;

/**
 * Verifies {@link JwtService} produces an RS256 token whose claims round-trip
 * through the RSA public-key decoder. No Spring context needed.
 */
class JwtServiceTest {

    private RsaKeyProvider keys;
    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        keys = new RsaKeyProvider();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(keys.jwkSet()));
        jwtService = new JwtService(encoder, Duration.ofHours(1));
        jwtDecoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
    }

    @Test
    void issue_producesVerifiableTokenWithUserClaims() {
        User user = TestUsers.user(UUID.randomUUID(), "alice@example.com", "Alice", Role.USER);

        JwtService.IssuedToken issued = jwtService.issue(user);

        Jwt decoded = jwtDecoder.decode(issued.token());
        assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
        assertThat(decoded.getClaimAsString("email")).isEqualTo("alice@example.com");
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Alice");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("payhub");
    }

    @Test
    void issue_setsExpiryFromTtl() {
        User user = TestUsers.user(UUID.randomUUID(), "bob@example.com", "Bob", Role.ADMIN);

        JwtService.IssuedToken issued = jwtService.issue(user);

        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(issued.expiresAt()).isBefore(Instant.now().plus(Duration.ofHours(2)));
    }
}
