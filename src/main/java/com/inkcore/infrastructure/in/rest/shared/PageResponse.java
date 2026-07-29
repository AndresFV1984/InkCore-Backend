package com.inkcore.infrastructure.in.rest.shared;

import com.inkcore.domain.shared.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(name = "PageResponse", description = """
        Listado paginado.
        content = elementos de la página; page 0-based; size = tamaño pedido;
        totalElements / totalPages / hasNext = metadatos de paginación.
        """)
public record PageResponse<T>(
        @Schema(description = "Elementos de la página actual")
        List<T> content,
        @Schema(description = "Página actual (0-based)", example = "0")
        int page,
        @Schema(description = "Tamaño de página", example = "20")
        int size,
        @Schema(description = "Total de elementos", example = "150")
        long totalElements,
        @Schema(description = "Total de páginas", example = "8")
        int totalPages,
        @Schema(description = "true si existe página siguiente", example = "true")
        boolean hasNext
) {
    public static <T, R> PageResponse<R> from(PageResult<T> page, Function<T, R> mapper) {
        List<R> mapped = page.content().stream().map(mapper).toList();
        return new PageResponse<>(
                mapped,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }
}
