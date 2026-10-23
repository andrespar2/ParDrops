package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.NotificationType;

import java.time.Instant;

/**
 * Representación de una notificación dentro de la aplicación.
 *
 * @param id        identificador del aviso.
 * @param type      tipo del aviso.
 * @param title     título corto.
 * @param message   texto explicativo.
 * @param link      ruta interna a la que lleva el aviso (opcional).
 * @param read      indica si el usuario ya lo leyó.
 * @param createdAt fecha de creación del aviso.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        Instant createdAt
) {
}
