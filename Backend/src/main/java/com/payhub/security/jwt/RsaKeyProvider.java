package com.payhub.security.jwt;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Holds the RSA key pair used to sign (private key) and verify (public key)
 * PayHub-issued JWTs, and to publish the public key via JWKS.
 *
 * <p>If {@code payhub.security.jwt.private-key}/{@code public-key} are set
 * (Base64-encoded PKCS#8 and X.509 DER), that fixed key pair is used, so tokens
 * survive restarts and multiple instances share the same key. Otherwise a key
 * pair is generated in memory — fine for local dev, but tokens are then
 * invalidated on every restart.
 */
@Component
public class RsaKeyProvider {

    private final RSAKey rsaKey;

    public RsaKeyProvider(
            @Value("${payhub.security.jwt.private-key:}") String privateKeyBase64,
            @Value("${payhub.security.jwt.public-key:}") String publicKeyBase64) {
        KeyPair keyPair =
                StringUtils.hasText(privateKeyBase64) && StringUtils.hasText(publicKeyBase64)
                        ? loadKeyPair(privateKeyBase64.trim(), publicKeyBase64.trim())
                        : generateRsaKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        this.rsaKey = new RSAKey.Builder(publicKey)
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(keyId(publicKey))
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

    private static KeyPair loadKeyPair(String privateKeyBase64, String publicKeyBase64) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid configured RSA JWT key material", e);
        }
    }

    /** Stable key id derived from the modulus, so the JWKS 'kid' is consistent. */
    private static String keyId(RSAPublicKey publicKey) {
        return Integer.toHexString(publicKey.getModulus().hashCode());
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
