package com.quasar.ledger.common.api;

import com.quasar.ledger.common.exception.ApiException;

/** Validated paging parameters: page is zero-based, size is 1..100 (default 20). */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static PageQuery of(Integer page, Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "page must be 0 or greater");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "size must be between 1 and " + MAX_SIZE);
        }
        return new PageQuery(p, s);
    }

    public long offset() {
        return (long) page * size;
    }
}
