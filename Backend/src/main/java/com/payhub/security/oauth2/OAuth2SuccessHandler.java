package com.payhub.security.oauth2;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.payhub.security.jwt.JwtService;
import com.payhub.security.jwt.JwtService.IssuedToken;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * After a successful Google login, issues a PayHub JWT for the provisioned user
 * and redirects the browser back to the Angular app, passing the token in the
 * URL fragment (never a query string, so it is not sent to servers or logged).
 * The SPA reads the fragment, stores the token and clears it from the URL.
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final String redirectUri;

    public OAuth2SuccessHandler(
            UserService userService,
            JwtService jwtService,
            @Value("${payhub.oauth2.success-redirect:http://localhost:4200/oauth2/callback}") String redirectUri) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        UUID userId = UUID.fromString(principal.getAttribute("uid"));
        User user = userService.getById(userId);

        IssuedToken issued = jwtService.issue(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .fragment("token=" + issued.token())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
