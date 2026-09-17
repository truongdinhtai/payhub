package com.payhub.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;

/** Public representation of a {@link User} returned by the API. */
public record UserResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCreatedAt());
    }
}
