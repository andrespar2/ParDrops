package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Identificadores numéricos para una acción en masa.
 *
 * @param ids identificadores seleccionados.
 */
public record BulkIdsRequest(
        @NotEmpty(message = "Selecciona al menos un elemento")
        @Size(max = 100, message = "Puedes actuar sobre 100 elementos como máximo")
        List<Long> ids
) {
}
