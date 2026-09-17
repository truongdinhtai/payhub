package com.payhub.security.jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Component;

/**
 * Holds the RSA key pair used to sign (private key) and verify (public key)
 * PayHub-issued JWTs, and to publish the public key via JWKS.
 *
 * <p>For simplicity the key pair is generated in memory on startup, which means
 * tokens are invalidated on restart. In production the key material should be
 * externalised (e.g. mounted PEM / secret) so restarts and multiple instances
 * share a stable key.
 */
@Component
public class RsaKeyProvider {

    private final RSAKey rsaKey;

    public RsaKeyProvider() {
        KeyPair keyPair = generateRsaKeyPair();
        this.rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    /** Full key (public + private) used for signing. */
    public RSAKey rsaKey() {
        return rsaKey;
    }

    public RSAPublicKey publicKey() {
        try {
            return rsaKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot expose RSA public key", e);
        }
    }

    /** JWK set with the signing key, consumed by the JWT encoder. */
    public JWKSet jwkSet() {
        return new JWKSet(rsaKey);
    }

    /** JWK set containing only the public key, for the JWKS endpoint. */
    public JWKSet publicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair generation failed", e);
        }
    }
}
