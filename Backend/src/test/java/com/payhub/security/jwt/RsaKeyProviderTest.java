package com.payhub.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class RsaKeyProviderTest {

    @Test
    void generatesKeyPair_whenNotConfigured() {
        RsaKeyProvider provider = new RsaKeyProvider("", "");
        assertThat(provider.publicKey()).isNotNull();
    }

    @Test
    void loadsConfiguredKeyPair_andSignsVerifiableTokens() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        // Java encodes RSA keys as PKCS#8 (private) and X.509 (public) DER — the
        // exact format the provider expects and that our openssl keys use.
        String privateB64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String publicB64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        RsaKeyProvider provider = new RsaKeyProvider(privateB64, publicB64);

        // The configured public key is used as-is.
        assertThat(provider.publicKey()).isEqualTo(keyPair.getPublic());

        // A token signed with the configured key verifies with the configured key.
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(provider.jwkSet()));
        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(provider.publicKey()).build();

        String token = encoder.encode(JwtEncoderParameters.from(
                JwtClaimsSet.builder().subject("user-1").build())).getTokenValue();
        Jwt decoded = decoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo("user-1");
    }
}
