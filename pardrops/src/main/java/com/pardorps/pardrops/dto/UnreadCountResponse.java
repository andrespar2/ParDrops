package com.pardorps.pardrops.dto;

/**
 * Número de notificaciones pendientes de leer.
 *
 * @param unreadCount cantidad de avisos sin leer.
 */
public record UnreadCountResponse(long unreadCount) {
}
