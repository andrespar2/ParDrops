package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.AuditAction;

import java.time.Instant;

/**
 * Representación de un registro de la bitácora de auditoría.
 *
 * @param id         identificador del registro.
 * @param actorName  nombre del administrador que ejecutó la acción.
 * @param action     tipo de acción ejecutada.
 * @param entityType tipo de entidad afectada.
 * @param entityId   identificador de la entidad afectada.
 * @param details    detalle legible de la acción.
 * @param createdAt  fecha de la acción.
 */
public record AuditLogResponse(
        Long id,
        String actorName,
        AuditAction action,
        String entityType,
        String entityId,
        String details,
        Instant createdAt
) {
}
