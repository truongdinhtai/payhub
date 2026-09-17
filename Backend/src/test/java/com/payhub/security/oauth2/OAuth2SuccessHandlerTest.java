package com.payhub.security.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.payhub.security.jwt.JwtService;
import com.payhub.support.TestUsers;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Test
    void onAuthenticationSuccess_redirectsToFrontendWithTokenFragment() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = TestUsers.user(userId, "dave@example.com", "Dave", Role.USER);
        when(userService.getById(userId)).thenReturn(user);
        when(jwtService.issue(user))
                .thenReturn(new JwtService.IssuedToken("signed.jwt.token", Instant.now().plusSeconds(3600)));

        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-1", "uid", userId.toString()),
                "sub");
        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");

        OAuth2SuccessHandler handler =
                new OAuth2SuccessHandler(userService, jwtService, "http://localhost:4200/oauth2/callback");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:4200/oauth2/callback#token=signed.jwt.token");
    }
}
