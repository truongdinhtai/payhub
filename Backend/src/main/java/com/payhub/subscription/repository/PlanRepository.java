package com.payhub.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;

public interface PlanRepository extends JpaRepository<Plan, PlanCode> {
}
