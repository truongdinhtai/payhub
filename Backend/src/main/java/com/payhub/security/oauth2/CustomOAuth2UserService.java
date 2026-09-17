package com.payhub.security.oauth2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * Bridges Google OAuth2 login to the local user store. Delegates the actual
 * userinfo fetch to a {@link DefaultOAuth2UserService}, then upserts the
 * profile and enriches the principal with the internal user id ({@code uid})
 * and role so the success handler can issue a token.
 *
 * <p>Uses composition (a delegate) rather than inheritance so the mapping logic
 * is unit-testable without calling Google.
 */
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final UserService userService;

    @Autowired
    public CustomOAuth2UserService(UserService userService) {
        this(new DefaultOAuth2UserService(), userService);
    }

    CustomOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
                            UserService userService) {
        this.delegate = delegate;
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);

        String googleSub = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String avatarUrl = oauthUser.getAttribute("picture");

        User user = userService.upsertFromGoogle(googleSub, email, name, avatarUrl);

        Map<String, Object> attributes = new HashMap<>(oauthUser.getAttributes());
        attributes.put("uid", user.getId().toString());
        attributes.put("role", user.getRole().name());

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return new DefaultOAuth2User(authorities, attributes, "sub");
    }
}
