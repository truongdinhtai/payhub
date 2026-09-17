package com.payhub.subscription.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.subscription.dto.PlanResponse;
import com.payhub.subscription.service.PlanService;

/** Exposes the subscription plan catalog. */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Subscription plan catalog")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "List plans", security = @SecurityRequirement(name = "bearer-jwt"))
    public List<PlanResponse> list() {
        return planService.getAll().stream().map(PlanResponse::from).toList();
    }
}
