package com.payhub.subscription.dto;

import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;

/**
 * Public catalog view of a plan. {@code maxProjects}/{@code maxSeats} of
 * {@code -1} mean unlimited.
 */
public record PlanResponse(
        PlanCode code,
        String name,
        String description,
        int priceCents,
        String currency,
        int maxProjects,
        int maxSeats) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPriceCents(),
                plan.getCurrency(),
                plan.getMaxProjects(),
                plan.getMaxSeats());
    }
}
