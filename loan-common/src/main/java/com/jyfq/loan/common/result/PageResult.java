package com.jyfq.loan.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Simple page result wrapper for admin queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private long current;
    private long size;
    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }

    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(current, size, 0L, Collections.emptyList());
    }
}
