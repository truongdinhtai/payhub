package com.payhub.common.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Stable, framework-agnostic pagination envelope returned to the Angular client.
 * Decouples the API contract from Spring Data's {@link Page} serialization.
 *
 * @param content       page items
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total number of matching items across all pages
 * @param totalPages    total number of pages
 * @param last          whether this is the last page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
