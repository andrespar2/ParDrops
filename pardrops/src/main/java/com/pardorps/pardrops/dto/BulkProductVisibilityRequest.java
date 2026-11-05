package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cambio de visibilidad para varios productos.
 *
 * @param ids    productos seleccionados.
 * @param active {@code true} para publicar, {@code false} para ocultar.
 */
public record BulkProductVisibilityRequest(
        @NotEmpty(message = "Selecciona al menos un producto")
        @Size(max = 100, message = "Puedes actuar sobre 100 productos como máximo")
        List<String> ids,
        boolean active
) {
}
