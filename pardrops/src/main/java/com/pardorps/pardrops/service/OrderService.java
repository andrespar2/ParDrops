package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.dto.OrderItemRequest;
import com.pardorps.pardrops.dto.OrderItemResponse;
import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.dto.OrderSubOrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.FundsStatus;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio con la lógica de negocio de los pedidos.
 *
 * <p>Los precios siempre se calculan a partir del catálogo del servidor, nunca
 * desde los datos enviados por el cliente, para evitar manipulaciones.</p>
 *
 * <p>En un marketplace un pedido puede reunir productos de varias tiendas, así
 * que al registrarlo se agrupan las líneas por vendedor y se genera un
 * subpedido por cada grupo. Cada tienda gestiona solo su parte y el estado
 * global del pedido se calcula a partir de los subpedidos.</p>
 */
@Service
public class OrderService {

    /** Porcentaje de IVA aplicado en Colombia (19%). */
    private static final double TAX_RATE = 0.19;

    /** Importe a partir del cual el envío es gratuito (400.000 COP). */
    private static final double FREE_SHIPPING_THRESHOLD = 400_000.0;

    /** Coste de envío cuando no se alcanza el mínimo (25.900 COP). */
    private static final double SHIPPING_COST = 25_900.0;

    /** Nombre de la tienda propia de la plataforma (productos sin vendedor). */
    private static final String PLATFORM_STORE_NAME = "ParDrops";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final NotificationService notificationService;
    private final OrderEmailService orderEmailService;
    private final SellerBalanceService sellerBalanceService;
    private final double commissionRate;

    /**
     * @param orderRepository      acceso a los pedidos.
     * @param productRepository    acceso al catálogo para calcular precios.
     * @param storeRepository      acceso a las tiendas para nombrar cada parte.
     * @param notificationService  avisos dentro de la aplicación.
     * @param orderEmailService    correos del ciclo del pedido.
     * @param sellerBalanceService saldos y comisiones de los vendedores.
     * @param commissionRate       comisión de la plataforma.
     */
    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            NotificationService notificationService,
            OrderEmailService orderEmailService,
            SellerBalanceService sellerBalanceService,
            @Value("${app.marketplace.commission-rate:0.05}") double commissionRate
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.notificationService = notificationService;
        this.orderEmailService = orderEmailService;
        this.sellerBalanceService = sellerBalanceService;
        this.commissionRate = commissionRate;
    }

    /**
     * Registra un pedido a partir de los datos del checkout.
     *
     * <p>Las líneas se agrupan por vendedor: cada grupo genera un subpedido con
     * su propio estado e importes. El stock se descuenta dentro de la misma
     * transacción, de modo que si algo falla no se descuenta nada.</p>
     *
     * @param request  datos del cliente, dirección y productos.
     * @param user     usuario autenticado, o {@code null} si es un invitado.
     * @return el pedido creado.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, User user) {
        Order order = new Order();
        order.setUser(user);
        order.setCustomerName(request.customerName().trim());
        order.setCustomerEmail(request.customerEmail().trim().toLowerCase());
        order.setCustomerPhone(request.customerPhone().trim());
        order.setAddressStreet(request.addressStreet().trim());
        order.setAddressCity(request.addressCity().trim());
        order.setAddressPostalCode(request.addressPostalCode().trim());
        order.setAddressCountry(request.addressCountry().trim());
        order.setNotes(request.notes());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());

        double subtotal = 0.0;
        Map<Long, List<OrderItem>> itemsBySeller = new LinkedHashMap<>();

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> ApiException.notFound(
                            "Producto no encontrado: " + itemRequest.productId()));

            if (!product.isActive()) {
                throw ApiException.conflict(
                        "El producto " + product.getName() + " ya no está disponible");
            }

            double unitPrice = resolveUnitPrice(product, itemRequest.variantName());
            double lineSubtotal = unitPrice * itemRequest.quantity();
            subtotal += lineSubtotal;

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSlug(product.getSlug());
            item.setVariantName(itemRequest.variantName());
            item.setUnitPrice(round(unitPrice));
            item.setQuantity(itemRequest.quantity());
            item.setSubtotal(round(lineSubtotal));
            item.setSeller(product.getSeller());
            order.addItem(item);

            Long sellerId = product.getSeller() != null ? product.getSeller().getId() : null;
            itemsBySeller.computeIfAbsent(sellerId, key -> new ArrayList<>()).add(item);

            decreaseStock(product, itemRequest.quantity());
        }

        double tax = round(subtotal * TAX_RATE);
        double shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0.0 : SHIPPING_COST;

        order.setSubtotal(round(subtotal));
        order.setTax(tax);
        order.setShipping(shipping);
        order.setTotal(round(subtotal + tax + shipping));

        createSubOrders(order, itemsBySeller);
        OrderStatus globalStatus = OrderStatus.aggregate(
                order.getSubOrders().stream().map(OrderSubOrder::getStatus).toList());
        if (globalStatus != null) {
            order.setStatus(globalStatus);
        }

        Order saved = orderRepository.save(order);
        notifySellers(saved);
        notifyCustomer(saved, user);
        orderEmailService.sendNewOrderToSellers(saved);
        for (OrderSubOrder subOrder : saved.getSubOrders()) {
            sellerBalanceService.hold(subOrder);
        }

        return toResponse(saved);
    }

    /**
     * Crea una parte del pedido por cada grupo de líneas.
     *
     * <p>Los impuestos se reparten en proporción al importe de cada parte y el
     * envío se divide a partes iguales, de modo que la suma de las partes sea
     * exactamente el total del pedido.</p>
     *
     * @param order        pedido al que pertenecen las partes.
     * @param itemsBySeller líneas agrupadas por vendedor (la clave nula es la
     *                     tienda propia de la plataforma).
     */
    private void createSubOrders(Order order, Map<Long, List<OrderItem>> itemsBySeller) {
        List<OrderSubOrder> subOrders = new ArrayList<>();

        for (Map.Entry<Long, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            User seller = null;
            String storeName = PLATFORM_STORE_NAME;
            String storeSlug = null;

            if (entry.getKey() != null) {
                seller = entry.getValue().get(0).getSeller();
                Store store = storeRepository.findBySellerId(seller.getId()).orElse(null);
                if (store != null) {
                    storeName = store.getName();
                    storeSlug = store.getSlug();
                } else {
                    storeName = seller.getFullName();
                }
            }

            OrderSubOrder subOrder = new OrderSubOrder();
            subOrder.setSeller(seller);
            subOrder.setStoreName(storeName);
            subOrder.setStoreSlug(storeSlug);
            subOrder.setStatus(OrderStatus.PENDING);
            subOrder.setCreatedAt(Instant.now());
            order.addSubOrder(subOrder);

            double subOrderSubtotal = 0.0;
            for (OrderItem item : entry.getValue()) {
                subOrderSubtotal += item.getSubtotal();
                subOrder.addItem(item);
            }

            subOrder.setSubtotal(round(subOrderSubtotal));
            subOrders.add(subOrder);
        }

        distributeAmounts(order, subOrders);
    }

    /**
     * Reparte los impuestos y el envío entre las partes del pedido.
     *
     * @param order    pedido con los importes globales ya calculados.
     * @param subOrders partes del pedido.
     */
    private void distributeAmounts(Order order, List<OrderSubOrder> subOrders) {
        if (subOrders.isEmpty()) {
            return;
        }

        double orderSubtotal = order.getSubtotal();
        double distributedTax = 0.0;
        double distributedShipping = 0.0;
        int shippingParts = subOrders.size();

        for (int index = 0; index < subOrders.size(); index++) {
            OrderSubOrder subOrder = subOrders.get(index);
            boolean last = index == subOrders.size() - 1;

            double tax = last
                    ? round(order.getTax() - distributedTax)
                    : round(order.getTax() * subOrder.getSubtotal() / orderSubtotal);
            double shipping = last
                    ? round(order.getShipping() - distributedShipping)
                    : round(order.getShipping() / shippingParts);

            subOrder.setShipping(shipping);
            subOrder.setTotal(round(subOrder.getSubtotal() + tax + shipping));

            double commission = round(subOrder.getSubtotal() * commissionRate);
            subOrder.setCommissionRate(commissionRate);
            subOrder.setCommissionAmount(commission);
            subOrder.setSellerNetAmount(round(subOrder.getTotal() - commission));
            subOrder.setFundsStatus(FundsStatus.HELD);

            distributedTax += tax;
            distributedShipping += shipping;
        }
    }

    /**
     * Descuenta del inventario las unidades compradas.
     *
     * @param product  producto comprado.
     * @param quantity unidades compradas.
     * @throws ApiException si no hay stock suficiente (409).
     */
    private void decreaseStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw ApiException.conflict(
                    "No hay stock suficiente de " + product.getName());
        }
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);
    }

    /**
     * Avisa a cada vendedor de que tiene una parte nueva que preparar.
     *
     * @param order pedido recién registrado.
     */
    private void notifySellers(Order order) {
        for (OrderSubOrder subOrder : order.getSubOrders()) {
            if (subOrder.getSeller() == null) {
                continue;
            }
            notificationService.notify(
                    subOrder.getSeller(),
                    NotificationType.ORDER,
                    "Tienes una venta nueva",
                    "La tienda " + subOrder.getStoreName() + " tiene un pedido por preparar.",
                    "/vendedor/pedidos");
        }
    }

    /**
     * Avisa al cliente de que su pedido está registrado.
     *
     * @param order pedido recién registrado.
     * @param user  cliente autenticado (nulo en las compras de invitado).
     */
    private void notifyCustomer(Order order, User user) {
        if (user == null) {
            return;
        }
        notificationService.notify(
                user,
                NotificationType.ORDER,
                "Tu pedido está en camino",
                "Hemos registrado tu pedido. Te avisaremos cuando cada tienda lo envíe.",
                "/cuenta");
    }

    /**
     * Devuelve el historial de pedidos de un usuario.
     *
     * @param user usuario autenticado.
     * @return sus pedidos, del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(User user) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve todos los pedidos (uso exclusivo del administrador).
     *
     * @return todos los pedidos, del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cambia el estado de un pedido (uso exclusivo del administrador).
     *
     * <p>Solo se admiten los avances definidos por
     * {@link OrderStatus#canTransitionTo(OrderStatus)}: se puede avanzar por el
     * flujo de preparación o cancelar antes de la entrega, pero nunca
     * retroceder ni modificar un pedido ya entregado o cancelado.</p>
     *
     * @param orderId   identificador del pedido.
     * @param newStatus nuevo estado solicitado.
     * @return el pedido actualizado.
     * @throws ApiException si el pedido no existe (404) o el cambio no es válido (409).
     */
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Pedido no encontrado: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw ApiException.conflict(
                    "No se puede cambiar el pedido del estado " + currentStatus
                            + " a " + newStatus + ".");
        }

        order.setStatus(newStatus);

        // El administrador puede forzar el estado del pedido: en ese caso las
        // partes que siguen vivas avanzan con él para que la información que ve
        // el cliente siga siendo coherente.
        for (OrderSubOrder subOrder : order.getSubOrders()) {
            if (!subOrder.getStatus().isTerminal()) {
                subOrder.setStatus(newStatus);
                if (newStatus == OrderStatus.DELIVERED) {
                    sellerBalanceService.release(subOrder);
                } else if (newStatus == OrderStatus.CANCELLED) {
                    sellerBalanceService.cancel(subOrder);
                }
            }
        }

        Order saved = orderRepository.save(order);
        if (newStatus == OrderStatus.PAID) {
            orderEmailService.sendPaymentReceipt(saved);
        } else {
            orderEmailService.sendStatusUpdate(saved);
            notifyCustomerStatus(saved, newStatus);
        }
        return toResponse(saved);
    }

    /**
     * Avisa al cliente autenticado de un cambio de estado del pedido.
     *
     * @param order  pedido actualizado.
     * @param status estado nuevo.
     */
    private void notifyCustomerStatus(Order order, OrderStatus status) {
        if (order.getUser() == null) {
            return;
        }
        notificationService.notify(
                order.getUser(),
                NotificationType.ORDER,
                "Tu pedido cambió de estado",
                "El pedido #" + order.getId() + " pasó a "
                        + orderEmailService.statusLabel(status) + ".",
                "/cuenta");
    }

    /**
     * Determina el precio unitario de una línea.
     *
     * <p>Si se indica una variante que existe en el producto y tiene precio
     * propio, se usa ese; en caso contrario, el precio del producto.</p>
     *
     * @param product     producto comprado.
     * @param variantName nombre de la variante (puede ser nulo).
     * @return el precio unitario a aplicar.
     */
    private double resolveUnitPrice(Product product, String variantName) {
        if (variantName != null && product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                if (variantName.equals(variant.getName()) && variant.getPrice() != null) {
                    return variant.getPrice();
                }
            }
        }
        return product.getPrice();
    }

    /**
     * Redondea un importe a dos decimales.
     *
     * @param value importe a redondear.
     * @return el importe redondeado.
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Convierte una entidad de pedido a su representación pública.
     *
     * @param order pedido a convertir.
     * @return el pedido listo para enviar al cliente.
     */
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
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

        List<OrderSubOrderResponse> subOrders = order.getSubOrders().stream()
                .map(this::toSubOrderResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getAddressStreet(),
                order.getAddressCity(),
                order.getAddressPostalCode(),
                order.getAddressCountry(),
                order.getNotes(),
                items,
                subOrders,
                order.getSubtotal(),
                order.getTax(),
                order.getShipping(),
                order.getTotal(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUser() != null ? order.getUser().getEmail() : null
        );
    }

    /**
     * Convierte una parte del pedido en su representación pública.
     *
     * @param subOrder parte a convertir.
     * @return la parte lista para enviar al cliente.
     */
    private OrderSubOrderResponse toSubOrderResponse(OrderSubOrder subOrder) {
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

        User seller = subOrder.getSeller();

        return new OrderSubOrderResponse(
                subOrder.getId(),
                subOrder.getStoreName(),
                subOrder.getStoreSlug(),
                seller != null ? seller.getFullName() : null,
                subOrder.getStatus(),
                subOrder.getSubtotal(),
                subOrder.getShipping(),
                subOrder.getTotal(),
                items,
                subOrder.getCreatedAt()
        );
    }
}
