package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.UserStatus;

import java.time.Instant;

/**
 * Representación de un vendedor en el panel de administración.
 *
 * <p>Reúne los datos de la cuenta, su tienda y sus cifras de actividad
 * (productos y ventas) para que la sección de vendedores pueda mostrarlos y
 * permitir suspender o reactivar la tienda.</p>
 *
 * @param sellerId       identificador del vendedor.
 * @param fullName       nombre del vendedor.
 * @param email          correo del vendedor.
 * @param phone          teléfono del vendedor.
 * @param accountStatus  estado de la cuenta del vendedor.
 * @param storeId        identificador de la tienda.
 * @param storeName      nombre comercial de la tienda.
 * @param storeSlug      slug público de la tienda.
 * @param storeStatus    estado de la tienda.
 * @param productCount   productos publicados por el vendedor.
 * @param salesCount     partes de pedido recibidas.
 * @param salesTotal     importe total vendido.
 * @param createdAt      fecha de alta de la cuenta.
 */
public record AdminSellerResponse(
        Long sellerId,
        String fullName,
        String email,
        String phone,
        UserStatus accountStatus,
        Long storeId,
        String storeName,
        String storeSlug,
        StoreStatus storeStatus,
        long productCount,
        long salesCount,
        double salesTotal,
        Instant createdAt
) {
}
