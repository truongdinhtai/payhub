package com.payhub.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.payhub.common.exception.ForbiddenResourceException;

/**
 * Resolves the currently authenticated user's id from the JWT in the security
 * context. This is the single source of tenant identity used to scope all
 * per-user data access.
 */
@Component
public class CurrentUserProvider {

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new ForbiddenResourceException("No authenticated user in security context");
    }
}
