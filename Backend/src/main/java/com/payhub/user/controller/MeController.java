package com.payhub.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.security.CurrentUserProvider;
import com.payhub.user.dto.UpdateProfileRequest;
import com.payhub.user.dto.UserResponse;
import com.payhub.user.service.UserService;

/** Returns the profile of the currently authenticated user. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "User", description = "Current user profile")
public class MeController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    public MeController(CurrentUserProvider currentUserProvider, UserService userService) {
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", security = @SecurityRequirement(name = "bearer-jwt"))
    public UserResponse me() {
        return UserResponse.from(userService.getById(currentUserProvider.currentUserId()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user's profile", security = @SecurityRequirement(name = "bearer-jwt"))
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(
                userService.updateName(currentUserProvider.currentUserId(), request.name()));
    }
}
