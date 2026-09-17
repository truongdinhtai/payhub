package com.payhub.user.domain;

/**
 * Coarse-grained authorization role. Mapped to a Spring Security authority
 * ({@code ROLE_USER} / {@code ROLE_ADMIN}) by the JWT authentication converter.
 */
public enum Role {
    USER,
    ADMIN
}
