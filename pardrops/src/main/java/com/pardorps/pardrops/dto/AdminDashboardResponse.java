package com.pardorps.pardrops.dto;

import java.util.List;

/**
 * Resumen general de la plataforma para el panel de administración.
 *
 * <p>Reúne en una sola respuesta los indicadores de la sección "Resumen
 * general": tamaño del marketplace, actividad comercial y avisos que requieren
 * atención del administrador.</p>
 *
 * @param userCount           cuentas registradas.
 * @param customerCount       cuentas con rol CLIENTE.
 * @param sellerCount         cuentas con rol SELLER.
 * @param activeSellerCount   vendedores con tienda activa.
 * @param suspendedUserCount  cuentas suspendidas.
 * @param productCount        productos del catálogo.
 * @param publishedProductCount productos publicados.
 * @param outOfStockCount     productos sin existencias.
 * @param storeCount          tiendas registradas.
 * @param activeStoreCount    tiendas activas.
 * @param orderCount          pedidos realizados.
 * @param pendingOrderCount   partes de pedido sin finalizar.
 * @param revenue             importe total vendido (suma de pedidos).
 * @param pendingApplicationCount solicitudes de vendedor por revisar.
 * @param recentOrders        últimos pedidos registrados.
 * @param topStores           tiendas con más ventas.
 */
public record AdminDashboardResponse(
        long userCount,
        long customerCount,
        long sellerCount,
        long activeSellerCount,
        long suspendedUserCount,
        long productCount,
        long publishedProductCount,
        long outOfStockCount,
        long storeCount,
        long activeStoreCount,
        long orderCount,
        long pendingOrderCount,
        double revenue,
        double commissionRevenue,
        double retainedBalance,
        double availableBalance,
        long pendingApplicationCount,
        List<AdminDashboardOrder> recentOrders,
        List<AdminDashboardStore> topStores
) {

    /**
     * Último pedido registrado en la plataforma.
     *
     * @param id          identificador del pedido.
     * @param customerName nombre del cliente.
     * @param total       importe total.
     * @param status      estado actual.
     * @param createdAt   fecha del pedido.
     */
    public record AdminDashboardOrder(
            Long id,
            String customerName,
            Double total,
            String status,
            java.time.Instant createdAt
    ) {
    }

    /**
     * Tienda con actividad comercial registrada.
     *
     * @param storeName  nombre de la tienda.
     * @param storeSlug  slug público de la tienda.
     * @param salesCount partes de pedido recibidas.
     * @param salesTotal importe total vendido.
     */
    public record AdminDashboardStore(
            String storeName,
            String storeSlug,
            long salesCount,
            double salesTotal
    ) {
    }
}
