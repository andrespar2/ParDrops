package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.NotificationResponse;
import com.pardorps.pardrops.dto.SellerDashboardResponse;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.SellerBalance;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que calcula el resumen del panel de un vendedor.
 *
 * <p>Todas las consultas se filtran por el identificador del vendedor
 * autenticado: las cifras de una tienda nunca incluyen datos de otra. El
 * cálculo se hace en el servidor para que la interfaz solo tenga que pintar el
 * resultado.</p>
 */
@Service
public class SellerDashboardService {

    /** Número máximo de avisos recientes que se devuelven en el resumen. */
    private static final int RECENT_NOTIFICATIONS = 5;

    private final OrderSubOrderRepository subOrderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final NotificationService notificationService;
    private final SellerBalanceRepository balanceRepository;

    /**
     * @param subOrderRepository  acceso a los subpedidos del vendedor.
     * @param productRepository   acceso a su catálogo.
     * @param storeRepository     acceso a la ficha comercial.
     * @param notificationService avisos del vendedor.
     * @param balanceRepository   saldo y comisiones del vendedor.
     */
    public SellerDashboardService(
            OrderSubOrderRepository subOrderRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            NotificationService notificationService,
            SellerBalanceRepository balanceRepository
    ) {
        this.subOrderRepository = subOrderRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.notificationService = notificationService;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Construye el resumen de actividad del vendedor autenticado.
     *
     * @param seller vendedor autenticado.
     * @return los indicadores de su panel.
     */
    @Transactional(readOnly = true)
    public SellerDashboardResponse loadSummary(User seller) {
        long orderCount = subOrderRepository.countBySellerId(seller.getId());
        long salesCount = subOrderRepository.countBySellerIdAndStatus(
                seller.getId(), OrderStatus.DELIVERED);
        long pendingOrderCount = subOrderRepository.countPendingBySellerId(
                seller.getId(), List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        double revenue = nz(subOrderRepository.sumNonCancelledBySellerId(
                seller.getId(), OrderStatus.CANCELLED));

        long publishedProductCount =
                productRepository.countPublishedBySellerId(seller.getId());

        long outOfStockCount =
                productRepository.countOutOfStockBySellerId(seller.getId());

        Store store = storeRepository.findBySellerId(seller.getId()).orElse(null);

        SellerBalance balance = balanceRepository.findBySellerId(seller.getId()).orElse(null);
        double commissionTotal = balance == null ? 0.0 : nz(balance.getCommissionTotal());
        double pendingBalance = balance == null ? 0.0 : nz(balance.getPendingAmount());
        double availableBalance = balance == null ? 0.0 : nz(balance.getAvailableAmount());
        double netRevenue = round(pendingBalance + availableBalance);

        List<NotificationResponse> recentNotifications =
                notificationService.findMine(seller).stream()
                        .limit(RECENT_NOTIFICATIONS)
                        .toList();

        return new SellerDashboardResponse(
                store == null ? null : store.getName(),
                store == null ? null : store.getSlug(),
                salesCount,
                orderCount,
                pendingOrderCount,
                publishedProductCount,
                outOfStockCount,
                revenue,
                commissionTotal,
                netRevenue,
                pendingBalance,
                availableBalance,
                recentNotifications
        );
    }

    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
