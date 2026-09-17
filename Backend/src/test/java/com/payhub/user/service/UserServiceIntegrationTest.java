package com.payhub.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;
import com.payhub.user.repository.UserRepository;

/**
 * Exercises {@link UserService#upsertFromGoogle} against a real database:
 * first-login creation, repeated-login update, and account linking by email.
 */
@SpringBootTest
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void firstLogin_createsUserWithDefaultRole() {
        User user = userService.upsertFromGoogle("sub-1", "new@example.com", "New User", "pic");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void repeatedLogin_updatesSameUserInsteadOfDuplicating() {
        User first = userService.upsertFromGoogle("sub-1", "user@example.com", "Old Name", "old-pic");

        User second = userService.upsertFromGoogle("sub-1", "user@example.com", "New Name", "new-pic");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getName()).isEqualTo("New Name");
        assertThat(second.getAvatarUrl()).isEqualTo("new-pic");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void existingEmailWithNewSub_linksToSameAccount() {
        User original = userService.upsertFromGoogle("sub-old", "link@example.com", "Linker", null);

        User linked = userService.upsertFromGoogle("sub-new", "link@example.com", "Linker", null);

        assertThat(linked.getId()).isEqualTo(original.getId());
        assertThat(linked.getGoogleSub()).isEqualTo("sub-new");
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
