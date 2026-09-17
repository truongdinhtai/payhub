package com.payhub.subscription.controller;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.security.CurrentUserProvider;
import com.payhub.subscription.dto.PlanSelectionRequest;
import com.payhub.subscription.dto.SubscribeResponse;
import com.payhub.subscription.dto.SubscriptionResponse;
import com.payhub.subscription.service.SubscriptionService;

/**
 * Manages the current user's subscription. Tenant identity always comes from
 * the JWT via {@link CurrentUserProvider}, never from the request body or path,
 * so a caller can only ever act on their own data.
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Subscribe, change plan and cancel")
@SecurityRequirement(name = "bearer-jwt")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  CurrentUserProvider currentUserProvider) {
        this.subscriptionService = subscriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subscribe to a plan (returns a Stripe checkout URL for paid plans)")
    public SubscribeResponse subscribe(@Valid @RequestBody PlanSelectionRequest request) {
        return SubscribeResponse.from(
                subscriptionService.subscribe(currentUserId(), request.planCode()));
    }

    @GetMapping("/current")
    @Operation(summary = "Get the current subscription")
    public SubscriptionResponse current() {
        return SubscriptionResponse.from(subscriptionService.getCurrent(currentUserId()));
    }

    @GetMapping
    @Operation(summary = "List subscription history")
    public List<SubscriptionResponse> history() {
        return subscriptionService.getHistory(currentUserId()).stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a subscription by id (owner only)")
    public SubscriptionResponse byId(@PathVariable UUID id) {
        return SubscriptionResponse.from(subscriptionService.getOwned(currentUserId(), id));
    }

    @PutMapping("/current/plan")
    @Operation(summary = "Change the current subscription's plan (upgrades return a Stripe checkout URL)")
    public SubscribeResponse changePlan(@Valid @RequestBody PlanSelectionRequest request) {
        return SubscribeResponse.from(
                subscriptionService.changePlan(currentUserId(), request.planCode()));
    }

    @DeleteMapping("/current")
    @Operation(summary = "Cancel the current subscription at period end")
    public SubscriptionResponse cancel() {
        return SubscriptionResponse.from(subscriptionService.cancel(currentUserId()));
    }

    private UUID currentUserId() {
        return currentUserProvider.currentUserId();
    }
}
