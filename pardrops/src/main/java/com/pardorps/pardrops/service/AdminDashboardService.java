package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminDashboardResponse;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SellerApplicationStatus;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.CategoryRepository;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.SellerApplicationRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio con los indicadores del resumen general del panel de administración.
 *
 * <p>Reúne la información que el administrador necesita de un vistazo: tamaño
 * del marketplace, actividad comercial y avisos que requieren su atención. Los
 * cálculos se apoyan en consultas de agregación para no traer todos los datos
 * a memoria.</p>
 */
@Service
public class AdminDashboardService {

    /** Número de pedidos recientes que se muestran en el resumen. */
    private static final int RECENT_ORDERS = 5;

    /** Número de tiendas destacadas por ventas que se muestran. */
    private static final int TOP_STORES = 5;

    /** Estados que no cuentan como trabajo pendiente. */
    private static final List<OrderStatus> CLOSED_STATUSES =
            List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final OrderSubOrderRepository subOrderRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final CategoryRepository categoryRepository;
    private final SellerBalanceRepository balanceRepository;

    /**
     * @param userRepository               acceso a las cuentas.
     * @param productRepository            acceso al catálogo.
     * @param storeRepository              acceso a las tiendas.
     * @param orderRepository              acceso a los pedidos.
     * @param subOrderRepository           acceso a las partes de pedido.
     * @param sellerApplicationRepository  acceso a las solicitudes de vendedor.
     * @param categoryRepository           acceso a las categorías.
     * @param balanceRepository            acceso a los saldos y comisiones.
     */
    public AdminDashboardService(
            UserRepository userRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            OrderRepository orderRepository,
            OrderSubOrderRepository subOrderRepository,
            SellerApplicationRepository sellerApplicationRepository,
            CategoryRepository categoryRepository,
            SellerBalanceRepository balanceRepository
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.subOrderRepository = subOrderRepository;
        this.sellerApplicationRepository = sellerApplicationRepository;
        this.categoryRepository = categoryRepository;
        this.balanceRepository = balanceRepository;
    }

    /**
     * Construye el resumen general de la plataforma.
     *
     * @return los indicadores y las listas del panel.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse summary() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByRole(Role.CUSTOMER),
                userRepository.countByRole(Role.SELLER),
                storeRepository.countByStatus(StoreStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.SUSPENDED),
                productRepository.count(),
                productRepository.findPublished().size(),
                productRepository.countOutOfStock(),
                storeRepository.count(),
                storeRepository.countByStatus(StoreStatus.ACTIVE),
                orderRepository.count(),
                orderRepository.countPending(CLOSED_STATUSES),
                orderRepository.sumTotal(),
                nz(balanceRepository.sumCommissionTotal()),
                nz(balanceRepository.sumPendingAmount()),
                nz(balanceRepository.sumAvailableAmount()),
                sellerApplicationRepository.countByStatus(SellerApplicationStatus.PENDING),
                recentOrders(orders),
                topStores()
        );
    }

    /**
     * Construye la lista de pedidos recientes.
     *
     * @param orders pedidos ordenados de más reciente a más antiguo.
     * @return los últimos pedidos registrados.
     */
    private List<AdminDashboardResponse.AdminDashboardOrder> recentOrders(List<Order> orders) {
        return orders.stream()
                .limit(RECENT_ORDERS)
                .map(order -> new AdminDashboardResponse.AdminDashboardOrder(
                        order.getId(),
                        order.getCustomerName(),
                        order.getTotal(),
                        order.getStatus().name(),
                        order.getCreatedAt()))
                .toList();
    }

    /**
     * Construye el ranking de tiendas con más ventas.
     *
     * <p>Se agrupan las partes de pedido por tienda y se ordenan por importe
     * facturado descendente.</p>
     *
     * @return las tiendas con mayor volumen de ventas.
     */
    private List<AdminDashboardResponse.AdminDashboardStore> topStores() {        Map<String, double[]> totals = new LinkedHashMap<>();
        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, String> slugs = new LinkedHashMap<>();

        for (OrderSubOrder subOrder : subOrderRepository.findAllForRanking()) {
            String name = subOrder.getStoreName();
            if (name == null) {
                continue;
            }
            totals.computeIfAbsent(name, key -> new double[1])[0] +=
                    subOrder.getTotal() == null ? 0 : subOrder.getTotal();
            counts.computeIfAbsent(name, key -> new long[1])[0]++;
            if (subOrder.getStoreSlug() != null) {
                slugs.putIfAbsent(name, subOrder.getStoreSlug());
            }
        }

        return totals.entrySet().stream()
                .sorted(Comparator.comparingDouble(
                        (Map.Entry<String, double[]> entry) -> entry.getValue()[0]).reversed())
                .limit(TOP_STORES)
                .map(entry -> new AdminDashboardResponse.AdminDashboardStore(
                        entry.getKey(),
                        slugs.get(entry.getKey()),
                        counts.get(entry.getKey())[0],
                        entry.getValue()[0]))
                .toList();
    }

    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }
}
