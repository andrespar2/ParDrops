package com.pardorps.pardrops.dto;

import java.util.List;

/**
 * Resumen de la actividad comercial de un vendedor.
 *
 * <p>Todas las cifras se calculan en el servidor a partir de los subpedidos y
 * los productos del vendedor autenticado, de modo que el panel nunca mezcla
 * datos de dos tiendas.</p>
 *
 * @param storeName               nombre comercial de la tienda (nulo si aún no existe).
 * @param storeSlug               slug público de la tienda (nulo si aún no existe).
 * @param salesCount              subpedidos entregados (ventas completadas).
 * @param orderCount              subpedidos recibidos en total.
 * @param pendingOrderCount       subpedidos que todavía no han finalizado.
 * @param publishedProductCount   productos publicados en el catálogo.
 * @param outOfStockCount         productos publicados sin existencias.
 * @param revenue                 importe acumulado de los subpedidos no cancelados.
 * @param recentNotifications     últimos avisos recibidos por el vendedor.
 */
public record SellerDashboardResponse(
        String storeName,
        String storeSlug,
        long salesCount,
        long orderCount,
        long pendingOrderCount,
        long publishedProductCount,
        long outOfStockCount,
        double revenue,
        List<NotificationResponse> recentNotifications
) {
}
