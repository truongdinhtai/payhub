package com.payhub.payment.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.payhub.payment.domain.TransactionStatus;
import com.payhub.payment.dto.TransactionSearchCriteria;
import com.payhub.payment.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionSearchServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionSearchService service;

    private final UUID userId = UUID.randomUUID();
    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    void search_normalizesBlankQueryAndMapsStatusToName() {
        when(transactionRepository.search(eq(userId), isNull(), eq("SUCCEEDED"), isNull(), isNull(),
                eq(pageable))).thenReturn(Page.empty());

        service.search(userId,
                new TransactionSearchCriteria("   ", TransactionStatus.SUCCEEDED, null, null), pageable);

        verify(transactionRepository).search(userId, null, "SUCCEEDED", null, null, pageable);
    }

    @Test
    void search_trimsQueryAndPassesNullStatus() {
        when(transactionRepository.search(eq(userId), eq("alice"), isNull(), isNull(), isNull(),
                eq(pageable))).thenReturn(Page.empty());

        service.search(userId,
                new TransactionSearchCriteria("  alice  ", null, null, null), pageable);

        verify(transactionRepository).search(userId, "alice", null, null, null, pageable);
    }
}
