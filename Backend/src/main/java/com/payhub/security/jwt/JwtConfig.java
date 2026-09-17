package com.payhub.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Wires the RSA-backed JWT encoder (signing) and decoder (verification) used by
 * PayHub as both token issuer and resource server.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(RsaKeyProvider keys) {
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(keys.jwkSet());
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RsaKeyProvider keys) {
        return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
    }
}
