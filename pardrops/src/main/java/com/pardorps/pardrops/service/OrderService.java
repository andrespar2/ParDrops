package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.dto.OrderItemRequest;
import com.pardorps.pardrops.dto.OrderItemResponse;
import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio con la lógica de negocio de los pedidos.
 *
 * <p>Los precios siempre se calculan a partir del catálogo del servidor, nunca
 * desde los datos enviados por el cliente, para evitar manipulaciones.</p>
 */
@Service
public class OrderService {

    /** Porcentaje de IVA aplicado (21%). */
    private static final double TAX_RATE = 0.21;

    /** Importe a partir del cual el envío es gratuito. */
    private static final double FREE_SHIPPING_THRESHOLD = 100.0;

    /** Coste de envío cuando no se alcanza el mínimo. */
    private static final double SHIPPING_COST = 5.99;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * @param orderRepository   acceso a los pedidos.
     * @param productRepository acceso al catálogo para calcular precios.
     */
    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Registra un pedido a partir de los datos del checkout.
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

        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> ApiException.notFound(
                            "Producto no encontrado: " + itemRequest.productId()));

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
            order.addItem(item);
        }

        double tax = round(subtotal * TAX_RATE);
        double shipping = subtotal >= FREE_SHIPPING_THRESHOLD ? 0.0 : SHIPPING_COST;

        order.setSubtotal(round(subtotal));
        order.setTax(tax);
        order.setShipping(shipping);
        order.setTotal(round(subtotal + tax + shipping));

        return toResponse(orderRepository.save(order));
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
                order.getSubtotal(),
                order.getTax(),
                order.getShipping(),
                order.getTotal(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUser() != null ? order.getUser().getEmail() : null
        );
    }
}
