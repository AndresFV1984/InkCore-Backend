package com.inkcore.domain.shared;

/**
 * Parámetros de paginación (0-based). size se acota a [1, {@value #MAX_SIZE}].
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static PageQuery of(Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageQuery(safePage, safeSize);
    }

    public int offset() {
        return page * size;
    }
}
