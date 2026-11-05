package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Identificadores de producto para una acción en masa.
 *
 * @param ids identificadores seleccionados.
 */
public record BulkProductIdsRequest(
        @NotEmpty(message = "Selecciona al menos un producto")
        @Size(max = 100, message = "Puedes actuar sobre 100 productos como máximo")
        List<String> ids
) {
}
