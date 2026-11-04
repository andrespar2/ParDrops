package com.pardorps.pardrops.payment;

import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.PlatformRevenue;
import com.pardorps.pardrops.model.PlatformRevenueType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.PlatformRevenueRepository;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.service.AuditLogService;
import com.pardorps.pardrops.service.NotificationService;
import com.pardorps.pardrops.service.OrderService;
import com.pardorps.pardrops.service.SellerBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * Aplica una transacción de Wompi al pedido y a los saldos.
 *
 * <p>Todas las operaciones financieras ocurren en una única transacción y son
 * idempotentes: si la transacción ya estaba aprobada no se vuelven a retener
 * saldos, no se registra dos veces el cargo de servicio y no se duplican los
 * avisos. Los correos se disparan después del commit.</p>
 */
@Service
public class PaymentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PlatformRevenueRepository revenueRepository;
    private final OrderService orderService;
    private final SellerBalanceService sellerBalanceService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * @param orderRepository        acceso a los pedidos.
     * @param userRepository         acceso a las cuentas.
     * @param revenueRepository      ingresos propios de la plataforma.
     * @param orderService           inventario y conversión de pedidos.
     * @param sellerBalanceService   saldos de los vendedores.
     * @param notificationService    avisos internos.
     * @param auditLogService        bitácora de auditoría.
     * @param eventPublisher         publicación del evento de compra pagada.
     */
    public PaymentApplicationService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            PlatformRevenueRepository revenueRepository,
            OrderService orderService,
            SellerBalanceService sellerBalanceService,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.revenueRepository = revenueRepository;
        this.orderService = orderService;
        this.sellerBalanceService = sellerBalanceService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Aplica una transacción verificada al pedido indicado.
     *
     * @param orderId     identificador del pedido.
     * @param transaction transacción consultada a Wompi.
     * @param source      origen de la verificación (webhook o consulta).
     * @return el resultado de la aplicación.
     */
    @Transactional
    public PaymentApplicationResult apply(Long orderId, WompiTransaction transaction, String source) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Pedido no encontrado: " + orderId));

        PaymentStatus received = parseStatus(transaction.status());
        if (received == null) {
            throw ApiException.badRequest("Wompi devolvió un estado desconocido");
        }

        if (!referenceMatches(order, transaction)) {
            throw ApiException.badRequest("La transacción no corresponde a la referencia del pedido");
        }
        if (!amountMatches(order, transaction)) {
            throw ApiException.badRequest("El monto de la transacción no coincide con el pedido");
        }

        if (received != PaymentStatus.APPROVED) {
            return recordNonApproved(order, transaction, received, source);
        }

        if (PaymentStatus.APPROVED.equals(order.getPaymentStatus())) {
            boolean notified = ensureInternalNotifications(order);
            // Permite reintentar correos que hayan quedado fallidos sin
            // duplicar los ya enviados (el registro de envíos lo impide).
            eventPublisher.publishEvent(new OrderPaidEvent(order.getId()));
            return new PaymentApplicationResult(
                    PaymentStatus.APPROVED, false, true, notified,
                    "La compra ya estaba confirmada");
        }

        order.setPaymentStatus(PaymentStatus.APPROVED);
        order.setWompiTransactionId(transaction.id());
        order.setPaymentMethod(transaction.paymentMethodType());
        order.setPaidAt(transaction.createdAt() != null ? transaction.createdAt() : Instant.now());

        boolean orderWasCancelled = order.getStatus() == OrderStatus.CANCELLED;
        if (!orderWasCancelled && order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.PAID);
            for (OrderSubOrder subOrder : order.getSubOrders()) {
                if (subOrder.getStatus() == OrderStatus.PENDING) {
                    subOrder.setStatus(OrderStatus.PAID);
                }
            }
        }

        if (!orderWasCancelled) {
            orderService.reserveStockForPaidOrder(order);
            for (OrderSubOrder subOrder : order.getSubOrders()) {
                if (subOrder.getSeller() != null && subOrder.getFundsStatus() == FundsStatus.HELD) {
                    sellerBalanceService.hold(subOrder);
                }
            }
        }

        registerServiceFee(order);
        boolean notified = ensureInternalNotifications(order);

        auditLogService.record(
                null,
                AuditAction.PAYMENT_APPROVED,
                "ORDER",
                String.valueOf(order.getId()),
                "Pago aprobado por Wompi (" + source + "). Transacción " + transaction.id()
                        + ", total " + order.getTotal() + " " + order.getCurrency());

        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderPaidEvent(saved.getId()));

        log.info("Pago aprobado para el pedido {} (transacción {})",
                saved.getId(), transaction.id());

        return new PaymentApplicationResult(
                PaymentStatus.APPROVED, true, false, notified,
                orderWasCancelled
                        ? "El pago se registró, pero el pedido estaba cancelado"
                        : "Pago confirmado");
    }

    /**
     * Registra un estado no aprobado sin tocar saldos ni confirmar la compra.
     *
     * @param order       pedido.
     * @param transaction transacción verificada.
     * @param status      estado recibido.
     * @param source      origen de la verificación.
     * @return el resultado.
     */
    private PaymentApplicationResult recordNonApproved(
            Order order,
            WompiTransaction transaction,
            PaymentStatus status,
            String source
    ) {
        if (PaymentStatus.APPROVED.equals(order.getPaymentStatus())) {
            return new PaymentApplicationResult(
                    PaymentStatus.APPROVED, false, true, false,
                    "La compra ya estaba confirmada; no se degrada el pago");
        }

        order.setPaymentStatus(status);
        order.setWompiTransactionId(transaction.id());
        order.setPaymentMethod(transaction.paymentMethodType());
        orderRepository.save(order);

        auditLogService.record(
                null,
                AuditAction.PAYMENT_STATUS_CHANGED,
                "ORDER",
                String.valueOf(order.getId()),
                "Estado de pago " + status + " notificado por Wompi (" + source + ")");

        log.info("Pago {} para el pedido {} (transacción {})",
                status, order.getId(), transaction.id());

        return new PaymentApplicationResult(
                status, false, false, false, "El pago no fue aprobado");
    }

    /**
     * Registra el cargo fijo de servicio una sola vez por pedido.
     *
     * @param order pedido aprobado.
     */
    private void registerServiceFee(Order order) {
        double fee = order.getServiceFee() == null ? 0.0 : order.getServiceFee();
        if (fee <= 0) {
            return;
        }
        boolean exists = revenueRepository
                .findByOrderIdAndType(order.getId(), PlatformRevenueType.SERVICE_FEE)
                .isPresent();
        if (exists) {
            return;
        }
        PlatformRevenue revenue = new PlatformRevenue();
        revenue.setOrderId(order.getId());
        revenue.setPaymentReference(order.getPaymentReference());
        revenue.setType(PlatformRevenueType.SERVICE_FEE);
        revenue.setAmount(fee);
        revenue.setCurrency(order.getCurrency());
        revenue.setWompiTransactionId(order.getWompiTransactionId());
        revenue.setDescription("Cargo de servicio ParDrops del pedido " + order.getId());
        revenueRepository.save(revenue);
    }

    /**
     * Crea los avisos internos de la compra confirmada una sola vez.
     *
     * @param order pedido aprobado.
     * @return {@code true} si se crearon en esta llamada.
     */
    private boolean ensureInternalNotifications(Order order) {
        if (order.getPaymentNotifiedAt() != null) {
            return false;
        }

        if (order.getUser() != null) {
            notificationService.notify(
                    order.getUser(),
                    NotificationType.ORDER,
                    "Pago confirmado",
                    "Tu pago fue aprobado y el pedido #" + order.getId() + " está registrado.",
                    "/cuenta");
        }

        for (OrderSubOrder subOrder : order.getSubOrders()) {
            User seller = subOrder.getSeller();
            if (seller == null) {
                continue;
            }
            notificationService.notify(
                    seller,
                    NotificationType.ORDER,
                    "Nueva venta confirmada",
                    "Se confirmó el pago del pedido #" + order.getId()
                            + " para " + subOrder.getStoreName()
                            + ". Importe: " + subOrder.getSellerNetAmount() + " COP.",
                    "/vendedor/pedidos");
        }

        String summary = "Pedido #" + order.getId() + " pagado por "
                + order.getTotal() + " COP. Referencia " + order.getPaymentReference() + ".";
        for (User admin : userRepository.findAll()) {
            if (admin.getRole() == Role.ADMIN && admin.getEmail() != null) {
                notificationService.notify(
                        admin,
                        NotificationType.ORDER,
                        "Nueva venta confirmada",
                        summary,
                        "/admin/pedidos");
            }
        }

        order.setPaymentNotifiedAt(Instant.now());
        return true;
    }

    private boolean referenceMatches(Order order, WompiTransaction transaction) {
        if (transaction.reference() == null || order.getPaymentReference() == null) {
            return false;
        }
        return order.getPaymentReference().equals(transaction.reference());
    }

    private boolean amountMatches(Order order, WompiTransaction transaction) {
        long expected = Math.round((order.getTotal() == null ? 0.0 : order.getTotal()) * 100.0);
        return expected == transaction.amountInCents();
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
