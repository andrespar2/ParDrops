package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.SupportStatus;

import java.time.Instant;

/**
 * Resumen de una conversación de soporte.
 *
 * @param id            identificador.
 * @param subject       asunto.
 * @param status        estado.
 * @param lastMessage   texto del último mensaje.
 * @param lastMessageAt fecha del último mensaje.
 * @param unread        hay mensajes sin leer para quien consulta.
 * @param createdAt     fecha de apertura.
 */
public record SupportConversationResponse(
        Long id,
        String subject,
        SupportStatus status,
        String lastMessage,
        Instant lastMessageAt,
        boolean unread,
        Instant createdAt
) {
}
