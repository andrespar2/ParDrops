package com.pardorps.pardrops.dto;

import java.util.List;

/**
 * Resultado de una acción en masa.
 *
 * @param requested número de elementos solicitados.
 * @param updated   elementos modificados.
 * @param skipped   elementos omitidos por no necesitar cambios.
 * @param errors    mensajes de los elementos que no se pudieron procesar.
 */
public record BulkActionResponse(
        int requested,
        int updated,
        int skipped,
        List<String> errors
) {
    public static BulkActionResponse of(int requested, int updated, int skipped) {
        return new BulkActionResponse(requested, updated, skipped, List.of());
    }
}
