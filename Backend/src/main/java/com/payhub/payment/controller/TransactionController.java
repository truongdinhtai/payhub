package com.payhub.payment.controller;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.common.dto.PageResponse;
import com.payhub.payment.domain.TransactionStatus;
import com.payhub.payment.dto.TransactionResponse;
import com.payhub.payment.dto.TransactionSearchCriteria;
import com.payhub.payment.service.TransactionSearchService;
import com.payhub.security.CurrentUserProvider;

/**
 * Transaction history with full-text search and filters, for the current user.
 * All parameters are optional; with none supplied it returns the newest
 * transactions.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Payment history and search")
@SecurityRequirement(name = "bearer-jwt")
public class TransactionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionSearchService transactionSearchService;
    private final CurrentUserProvider currentUserProvider;

    public TransactionController(TransactionSearchService transactionSearchService,
                                CurrentUserProvider currentUserProvider) {
        this.transactionSearchService = transactionSearchService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "Search my transactions",
            description = "Full-text search (q) over customer name/email/description, "
                    + "plus optional status and created-at range filters.")
    public PageResponse<TransactionResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        TransactionSearchCriteria criteria = new TransactionSearchCriteria(q, status, from, to);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return PageResponse.from(transactionSearchService
                .search(currentUserProvider.currentUserId(), criteria, pageRequest)
                .map(TransactionResponse::from));
    }
}
