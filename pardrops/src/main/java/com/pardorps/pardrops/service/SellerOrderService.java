package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.OrderItemResponse;
import com.pardorps.pardrops.dto.SellerOrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio con la lógica de los pedidos de un vendedor.
 *
 * <p>En un marketplace cada vendedor gestiona únicamente la parte del pedido
 * que le corresponde (su subpedido). Este servicio se encarga de listar esas
 * partes y de permitir que el vendedor avance su propio estado.</p>
 *
 * <p>El aislamiento entre vendedores no depende de la interfaz: las consultas
 * filtran por el vendedor autenticado, de modo que un subpedido ajeno no
 * existe a ojos de quien no lo posee.</p>
 */
@Service
public class SellerOrderService {

    private final OrderSubOrderRepository subOrderRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final OrderEmailService orderEmailService;
    private final SellerBalanceService sellerBalanceService;

    /**
     * @param subOrderRepository acceso a las partes de los pedidos.
     * @param orderRepository    acceso a los pedidos.
     * @param notificationService avisos dentro de la aplicación.
     * @param orderEmailService  correos del ciclo del pedido.
     * @param sellerBalanceService saldos y comisiones.
     */
    public SellerOrderService(
            OrderSubOrderRepository subOrderRepository,
            OrderRepository orderRepository,
            NotificationService notificationService,
            OrderEmailService orderEmailService,
            SellerBalanceService sellerBalanceService
    ) {
        this.subOrderRepository = subOrderRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
        this.orderEmailService = orderEmailService;
        this.sellerBalanceService = sellerBalanceService;
    }

    /**
     * Devuelve las partes de pedido que pertenecen al vendedor autenticado.
     *
     * @param seller vendedor autenticado.
     * @return listado de partes, de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<SellerOrderResponse> findMine(User seller) {
        return subOrderRepository.findBySellerId(seller.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Avanza el estado de una parte de pedido del vendedor.
     *
     * <p>La transición se valida en el servidor con
     * {@link OrderStatus#canTransitionTo(OrderStatus)}: no se admiten retrocesos
     * ni cambios sobre estados finales. Tras el cambio se recalcula el estado
     * global del pedido (el peor caso entre sus partes) y se avisa al
     * cliente.</p>
     *
     * @param seller    vendedor autenticado.
     * @param subOrderId identificador de la parte de pedido.
     * @param newStatus nuevo estado solicitado.
     * @return la parte de pedido actualizada.
     * @throws ApiException si la parte no existe o no es del vendedor (404) o si
     *                     el cambio de estado no es válido (409).
     */
    @Transactional
    public SellerOrderResponse updateStatus(User seller, Long subOrderId, OrderStatus newStatus) {
        OrderSubOrder subOrder = subOrderRepository.findByIdAndSellerId(subOrderId, seller.getId())
                .orElseThrow(() -> ApiException.notFound(
                        "No tienes el pedido " + subOrderId));

        OrderStatus currentStatus = subOrder.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw ApiException.conflict(
                    "No se puede cambiar el pedido del estado " + currentStatus
                            + " a " + newStatus + ".");
        }

        subOrder.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            sellerBalanceService.release(subOrder);
        } else if (newStatus == OrderStatus.CANCELLED) {
            sellerBalanceService.cancel(subOrder);
        }

        Order order = subOrder.getOrder();
        updateGlobalStatus(order);
        orderRepository.save(order);

        notifyCustomer(order, subOrder);

        if (newStatus == OrderStatus.PAID) {
            orderEmailService.sendPaymentReceipt(order, subOrder);
        } else {
            orderEmailService.sendStatusUpdate(order, subOrder);
        }

        return toResponse(subOrder);
    }

    /**
     * Recalcula el estado global del pedido a partir de sus partes.
     *
     * <p>Los pedidos anteriores a la evolución a marketplace no tienen partes:
     * en ese caso su estado se conserva tal cual.</p>
     *
     * @param order pedido a recalcular.
     */
    private void updateGlobalStatus(Order order) {
        List<OrderStatus> statuses = order.getSubOrders().stream()
                .map(OrderSubOrder::getStatus)
                .toList();

        OrderStatus globalStatus = OrderStatus.aggregate(statuses);
        if (globalStatus != null) {
            order.setStatus(globalStatus);
        }
    }

    /**
     * Avisa al cliente de que su pedido ha cambiado de estado.
     *
     * @param order    pedido al que pertenece la parte.
     * @param subOrder parte que ha cambiado de estado.
     */
    private void notifyCustomer(Order order, OrderSubOrder subOrder) {
        User customer = order.getUser();
        if (customer == null) {
            return;
        }

        notificationService.notify(
                customer,
                NotificationType.ORDER,
                "Tu pedido va avanzando",
                "La tienda " + subOrder.getStoreName() + " actualizó su parte del pedido a "
                        + subOrder.getStatus() + ".",
                "/cuenta"
        );
    }

    /**
     * Convierte una parte de pedido en la representación del vendedor.
     *
     * <p>Incluye los datos de envío del cliente porque el vendedor es quien
     * prepara y entrega su parte. Solo se usa para partes del propio
     * vendedor, así que nunca se filtran datos ajenos.</p>
     *
     * @param subOrder parte de pedido a convertir.
     * @return la parte lista para enviar al vendedor.
     */
    private SellerOrderResponse toResponse(OrderSubOrder subOrder) {
        Order order = subOrder.getOrder();

        List<OrderItemResponse> items = subOrder.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getProductSlug(),
                        item.getVariantName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new SellerOrderResponse(
                subOrder.getId(),
                order.getId(),
                subOrder.getStoreName(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getAddressStreet(),
                order.getAddressCity(),
                order.getAddressPostalCode(),
                order.getAddressCountry(),
                subOrder.getStatus(),
                subOrder.getSubtotal(),
                subOrder.getShipping(),
                subOrder.getTotal(),
                items,
                subOrder.getCreatedAt()
        );
    }
}
