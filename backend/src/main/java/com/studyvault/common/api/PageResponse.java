package com.studyvault.common.api;

import org.springframework.data.domain.Page;

import java.util.List;

/** Pagination envelope; the API never returns unbounded lists. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public static <E, T> PageResponse<T> of(Page<E> page, List<T> mappedContent) {
        return new PageResponse<>(mappedContent, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
