package com.payhub.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.payhub.support.TestUsers;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Mock
    private UserService userService;

    @Mock
    private OAuth2UserRequest userRequest;

    @Test
    void loadUser_upsertsUserAndEnrichesPrincipal() {
        OAuth2User googleUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                Map.of(
                        "sub", "google-123",
                        "email", "carol@example.com",
                        "name", "Carol",
                        "picture", "https://img/avatar.png"),
                "sub");
        when(delegate.loadUser(userRequest)).thenReturn(googleUser);

        UUID userId = UUID.randomUUID();
        User provisioned = TestUsers.user(userId, "carol@example.com", "Carol", Role.USER);
        when(userService.upsertFromGoogle(eq("google-123"), eq("carol@example.com"),
                eq("Carol"), any())).thenReturn(provisioned);

        CustomOAuth2UserService service = new CustomOAuth2UserService(delegate, userService);
        OAuth2User result = service.loadUser(userRequest);

        assertThat(result.<String>getAttribute("uid")).isEqualTo(userId.toString());
        assertThat(result.<String>getAttribute("role")).isEqualTo("USER");
        assertThat(result.getName()).isEqualTo("google-123"); // name attribute key = "sub"
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }
}
