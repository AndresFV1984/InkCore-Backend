package com.inkcore.domain.shared;

import java.util.List;

/**
 * Resultado paginado de dominio (independiente de Spring Data).
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / (double) size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }
}
