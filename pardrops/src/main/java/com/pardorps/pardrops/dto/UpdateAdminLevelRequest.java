package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.AdminLevel;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de nivel de una cuenta administrativa.
 *
 * @param level nuevo nivel (nunca OWNER: el propietario es único).
 */
public record UpdateAdminLevelRequest(
        @NotNull(message = "El nivel es obligatorio")
        AdminLevel level
) {
}
