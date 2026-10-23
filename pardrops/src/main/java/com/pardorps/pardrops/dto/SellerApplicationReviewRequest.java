package com.pardorps.pardrops.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Decisión del administrador sobre una solicitud de vendedor.
 *
 * @param approve    {@code true} para aprobar, {@code false} para rechazar.
 * @param reviewNotes motivo de la decisión (opcional al aprobar, obligatorio al rechazar).
 */
public record SellerApplicationReviewRequest(

        @NotNull(message = "Debes indicar si apruebas o rechazas la solicitud")
        Boolean approve,

        @Size(max = 500, message = "El comentario es demasiado extenso")
        String reviewNotes
) {
}
