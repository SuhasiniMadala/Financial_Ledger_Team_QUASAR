package com.quasar.ledger.common.api;

import java.util.List;
import java.util.function.Function;

/** Paged list response (SRS 3.4: page and size, default 20, max 100). */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> of(List<T> items, PageQuery query, long totalItems) {
        int pages = (int) ((totalItems + query.size() - 1) / query.size());
        return new PageResponse<>(items, query.page(), query.size(), totalItems, pages);
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(items.stream().map(mapper).toList(), page, size, totalItems, totalPages);
    }
}
