package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.UserStatus;

import java.time.Instant;

/**
 * Detalle de una cuenta en el panel de administración.
 *
 * <p>Amplía el listado con los datos de la tienda del vendedor y con sus
 * cifras de actividad (productos publicados y ventas), de modo que el panel
 * pueda mostrar la ficha completa con una sola consulta.</p>
 *
 * @param id             identificador de la cuenta.
 * @param email          correo de la cuenta.
 * @param fullName       nombre completo.
 * @param phone          teléfono de contacto.
 * @param role           rol asignado.
 * @param status         estado de la cuenta.
 * @param createdAt      fecha de alta de la cuenta.
 * @param storeId        identificador de la tienda (nulo si no es vendedor).
 * @param storeName      nombre de la tienda (nulo si no es vendedor).
 * @param storeSlug      slug público de la tienda (nulo si no es vendedor).
 * @param storeStatus    estado de la tienda (nulo si no es vendedor).
 * @param productCount   productos publicados por el vendedor.
 * @param salesCount     partes de pedido recibidas por el vendedor.
 * @param salesTotal     importe total vendido por el vendedor.
 */
public record AdminUserDetailResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        Instant createdAt,
        Long storeId,
        String storeName,
        String storeSlug,
        StoreStatus storeStatus,
        long productCount,
        long salesCount,
        double salesTotal
) {
}
