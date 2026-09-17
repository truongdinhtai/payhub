package com.payhub.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.repository.PlanRepository;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    @Test
    void getByCode_returnsPlan_whenFound() {
        Plan pro = new Plan(PlanCode.PRO, "Pro", "desc", 2900, "USD", 10, 5);
        when(planRepository.findById(PlanCode.PRO)).thenReturn(Optional.of(pro));

        assertThat(planService.getByCode(PlanCode.PRO)).isSameAs(pro);
    }

    @Test
    void getByCode_throws_whenMissing() {
        when(planRepository.findById(PlanCode.ENTERPRISE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.getByCode(PlanCode.ENTERPRISE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_returnsCatalog() {
        Plan free = new Plan(PlanCode.FREE, "Free", "desc", 0, "USD", 1, 1);
        when(planRepository.findAll()).thenReturn(List.of(free));

        assertThat(planService.getAll()).containsExactly(free);
    }
}
