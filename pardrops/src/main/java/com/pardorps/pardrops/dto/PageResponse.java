package com.pardorps.pardrops.dto;

import java.util.List;

/**
 * Página de resultados de una consulta paginada.
 *
 * <p>El panel de administración maneja volúmenes de datos que justifican
 * paginar. En lugar de exponer el tipo {@code Page} de Spring (cuya forma
 * cambia entre versiones y arrastra metadatos internos), se devuelve esta
 * estructura mínima y estable para el frontend.</p>
 *
 * @param content       elementos de la página actual.
 * @param page          número de página (empezando en 0).
 * @param size          tamaño de página solicitado.
 * @param totalElements número total de elementos que cumplen el filtro.
 * @param totalPages    número total de páginas.
 * @param <T>           tipo de los elementos.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Construye la respuesta a partir de los datos de una página.
     *
     * @param content       elementos de la página.
     * @param page          número de página.
     * @param size          tamaño de página.
     * @param totalElements total de elementos.
     * @param <T>           tipo de los elementos.
     * @return la página lista para enviar al cliente.
     */
    public static <T> PageResponse<T> of(
            List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
