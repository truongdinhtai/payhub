package com.payhub.subscription.dto;

import jakarta.validation.constraints.NotNull;

import com.payhub.subscription.domain.PlanCode;

/** Request body for subscribing to or switching to a plan. */
public record PlanSelectionRequest(
        @NotNull(message = "planCode is required") PlanCode planCode) {
}
