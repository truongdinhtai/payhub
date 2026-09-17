package com.payhub.user.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;
import com.payhub.user.repository.UserRepository;

/**
 * User provisioning and lookup. {@link #upsertFromGoogle} is the entry point
 * used by the OAuth2 login flow: it creates a user on first login and keeps the
 * profile in sync on subsequent logins, linking by Google subject or email.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertFromGoogle(String googleSub, String email, String name, String avatarUrl) {
        User user = userRepository.findByGoogleSub(googleSub)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> User.fromGoogleProfile(googleSub, email, name, avatarUrl));

        // Keep the profile in sync on every login (and link email on new sub).
        user.setGoogleSub(googleSub);
        user.setEmail(email);
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    @Transactional
    public User updateName(UUID id, String name) {
        User user = getById(id);
        user.setName(name);
        return userRepository.save(user);
    }
}
