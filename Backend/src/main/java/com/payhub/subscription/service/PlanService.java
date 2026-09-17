package com.payhub.subscription.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.repository.PlanRepository;

/** Read access to the subscription plan catalog. */
@Service
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> getAll() {
        return planRepository.findAll();
    }

    public Plan getByCode(PlanCode code) {
        return planRepository.findById(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Plan", code));
    }
}
