package com.payhub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.payhub.common.exception.ForbiddenResourceException;

class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserId_returnsSubjectFromJwt() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject(userId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claims(c -> c.put("scope", "read"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(provider.currentUserId()).isEqualTo(userId);
    }

    @Test
    void currentUserId_throwsWhenNoJwtPrincipal() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("someone", "creds"));

        assertThatThrownBy(provider::currentUserId)
                .isInstanceOf(ForbiddenResourceException.class);
    }
}
