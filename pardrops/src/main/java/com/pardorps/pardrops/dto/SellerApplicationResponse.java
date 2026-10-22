package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.SellerApplicationStatus;

import java.time.Instant;

/**
 * Representación de una solicitud para convertirse en vendedor.
 *
 * @param id          identificador de la solicitud.
 * @param businessName nombre comercial del negocio.
 * @param taxId       NIT o identificación tributaria.
 * @param description descripción del negocio.
 * @param phone       teléfono de contacto.
 * @param city        ciudad del negocio.
 * @param status      estado de la solicitud.
 * @param reviewNotes comentario del administrador.
 * @param applicantName nombre del usuario que solicitó ser vendedor.
 * @param applicantEmail correo del usuario que solicitó ser vendedor.
 * @param reviewedBy  nombre del administrador que revisó la solicitud.
 * @param reviewedAt  fecha de la revisión.
 * @param createdAt   fecha de envío de la solicitud.
 */
public record SellerApplicationResponse(
        Long id,
        String businessName,
        String taxId,
        String description,
        String phone,
        String city,
        SellerApplicationStatus status,
        String reviewNotes,
        String applicantName,
        String applicantEmail,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
}
