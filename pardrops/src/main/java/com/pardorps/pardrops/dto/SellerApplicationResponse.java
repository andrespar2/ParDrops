package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.SellerApplicationStatus;

import java.time.Instant;

/**
 * Representación de una solicitud para convertirse en vendedor.
 *
 * <p>Incluye datos públicos de la tienda y datos legales privados. El endpoint
 * público de tiendas no usa este DTO: solo el solicitante y el administrador
 * pueden verlo.</p>
 *
 * @param id             identificador de la solicitud.
 * @param businessName   nombre comercial del negocio.
 * @param taxId          NIT o identificación tributaria.
 * @param description    descripción del negocio.
 * @param phone          teléfono del negocio.
 * @param city           ciudad del negocio.
 * @param publicEmail    correo público de la tienda.
 * @param publicPhone    teléfono público de la tienda.
 * @param legalName      nombre legal del titular.
 * @param documentType   tipo de documento.
 * @param documentNumber número de documento.
 * @param legalAddress   dirección legal.
 * @param personalPhone  teléfono personal.
 * @param status         estado de la solicitud.
 * @param reviewNotes    comentario del administrador.
 * @param applicantName  nombre del usuario que solicitó ser vendedor.
 * @param applicantEmail correo del usuario que solicitó ser vendedor.
 * @param reviewedBy     nombre del administrador que revisó la solicitud.
 * @param reviewedAt     fecha de la revisión.
 * @param createdAt      fecha de envío de la solicitud.
 */
public record SellerApplicationResponse(
        Long id,
        String businessName,
        String taxId,
        String description,
        String phone,
        String city,
        String publicEmail,
        String publicPhone,
        String legalName,
        String documentType,
        String documentNumber,
        String legalAddress,
        String personalPhone,
        SellerApplicationStatus status,
        String reviewNotes,
        String applicantName,
        String applicantEmail,
        String reviewedBy,
        Instant reviewedAt,
        Instant createdAt
) {
}
