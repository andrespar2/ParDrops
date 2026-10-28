package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.mail.EmailTemplateRenderer;
import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Construye y envía los correos del ciclo del pedido.
 *
 * <p>Los modelos se generan con los datos históricos del pedido. Un fallo de
 * correo se absorbe en {@link EmailService} y nunca revierte la operación.</p>
 */
@Service
public class OrderEmailService {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailService.class);

    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmailTemplateService templateService;
    private final EmailService emailService;
    private final StoreRepository storeRepository;
    private final String frontendUrl;

    /**
     * @param templateService servicio de plantillas.
     * @param emailService    entrega de correos.
     * @param storeRepository acceso a la tienda del vendedor.
     * @param frontendUrl     URL base del frontend.
     */
    public OrderEmailService(
            EmailTemplateService templateService,
            EmailService emailService,
            StoreRepository storeRepository,
            @Value("${app.mail.frontend-url:http://localhost:4200}") String frontendUrl
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.storeRepository = storeRepository;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    /**
     * Envía al vendedor la orden de compra de cada subpedido.
     *
     * @param order pedido recién creado.
     */
    public void sendNewOrderToSellers(Order order) {
        for (OrderSubOrder subOrder : order.getSubOrders()) {
            if (subOrder.getSeller() == null) {
                continue;
            }
            String to = sellerEmail(subOrder);
            if (to == null || to.isBlank()) {
                log.warn("Subpedido {} sin correo de vendedor", subOrder.getId());
                continue;
            }
            send(order, subOrder, EmailTemplateCode.SELLER_NEW_ORDER, to,
                    Map.of("sellerOrdersUrl", frontendUrl + "/vendedor/pedidos"));
        }
    }

    /**
     * Renderiza y envía una plantilla con los datos del subpedido.
     *
     * @param order    pedido.
     * @param subOrder parte del pedido.
     * @param code     plantilla.
     * @param to       destinatario.
     * @param extra    variables adicionales.
     */
    protected void send(
            Order order,
            OrderSubOrder subOrder,
            EmailTemplateCode code,
            String to,
            Map<String, ?> extra
    ) {
        Map<String, Object> model = buildModel(order, subOrder);
        model.putAll(extra);
        templateService.renderIfActive(code, model)
                .ifPresent(rendered -> emailService.send(to, rendered.subject(), rendered.bodyHtml()));
    }

    /**
     * Envía al comprador el comprobante del pedido completo.
     *
     * @param order pedido pagado.
     */
    public void sendPaymentReceipt(Order order) {
        sendPaymentReceipt(order, null);
    }

    /**
     * Envía al comprador el comprobante de una parte del pedido.
     *
     * @param order    pedido pagado.
     * @param subOrder parte pagada, o {@code null} para el pedido completo.
     */
    public void sendPaymentReceipt(Order order, OrderSubOrder subOrder) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            return;
        }
        send(order, subOrder, EmailTemplateCode.BUYER_PAYMENT_RECEIPT,
                order.getCustomerEmail(), Map.of());
    }

    /**
     * Envía al comprador el aviso de un cambio de estado del pedido completo.
     *
     * @param order pedido actualizado.
     */
    public void sendStatusUpdate(Order order) {
        sendStatusUpdate(order, null);
    }

    /**
     * Envía al comprador el aviso de un cambio de estado de una parte.
     *
     * @param order    pedido actualizado.
     * @param subOrder parte actualizada, o {@code null} para el pedido completo.
     */
    public void sendStatusUpdate(Order order, OrderSubOrder subOrder) {
        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            return;
        }
        send(order, subOrder, EmailTemplateCode.BUYER_ORDER_STATUS,
                order.getCustomerEmail(), Map.of());
    }

    /**
     * Construye las variables comunes de la plantilla.
     *
     * @param order    pedido.
     * @param subOrder parte del pedido, o {@code null} para el pedido completo.
     * @return variables disponibles.
     */
    protected Map<String, Object> buildModel(Order order, OrderSubOrder subOrder) {
        List<OrderItem> items = subOrder == null ? order.getItems() : subOrder.getItems();
        double subtotal = subOrder == null ? nz(order.getSubtotal()) : nz(subOrder.getSubtotal());
        double tax = subOrder == null ? nz(order.getTax()) : tax(subOrder);
        double shipping = subOrder == null ? nz(order.getShipping()) : nz(subOrder.getShipping());
        double total = subOrder == null ? nz(order.getTotal()) : nz(subOrder.getTotal());
        OrderStatus status = subOrder == null ? order.getStatus() : subOrder.getStatus();
        User seller = subOrder == null ? null : subOrder.getSeller();

        Map<String, Object> model = new HashMap<>();
        model.put("orderId", order.getId());
        model.put("orderDate", formatDate(order.getCreatedAt()));
        model.put("customerName", order.getCustomerName());
        model.put("customerEmail", order.getCustomerEmail());
        model.put("customerPhone", order.getCustomerPhone());
        model.put("address", address(order));
        model.put("notes", order.getNotes() == null ? "" : order.getNotes());
        model.put("storeName", subOrder == null ? "ParDrops" : subOrder.getStoreName());
        model.put("sellerName", seller != null ? seller.getFullName() : "equipo de ParDrops");
        model.put("itemsTable", new EmailTemplateRenderer.SafeHtml(itemsTable(items)));
        model.put("subtotal", money(subtotal));
        model.put("tax", money(tax));
        model.put("shipping", money(shipping));
        model.put("total", money(total));
        model.put("status", status == null ? "" : status.name());
        model.put("statusLabel", statusLabel(status));
        model.put("accountUrl", frontendUrl + "/cuenta");
        model.put("sellerOrdersUrl", frontendUrl + "/vendedor/pedidos");
        return model;
    }

    /**
     * Devuelve el correo del vendedor: el de la tienda si es válido o el de la
     * cuenta.
     *
     * @param subOrder parte del pedido.
     * @return correo o nulo.
     */
    protected String sellerEmail(OrderSubOrder subOrder) {
        User seller = subOrder.getSeller();
        if (seller == null) {
            return null;
        }
        Store store = storeRepository.findBySellerId(seller.getId()).orElse(null);
        if (store != null && isValidEmail(store.getContactEmail())) {
            return store.getContactEmail().trim();
        }
        return seller.getEmail();
    }

    /** Construye la tabla HTML de productos. */
    private String itemsTable(List<OrderItem> items) {
        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"width:100%;border-collapse:collapse;font-size:14px;color:#374151;\">")
                .append("<thead><tr style=\"background:#f9fafb;\">")
                .append("<th align=\"left\" style=\"padding:10px 8px;font-size:12px;color:#6b7280;\">Producto</th>")
                .append("<th align=\"center\" style=\"padding:10px 8px;font-size:12px;color:#6b7280;\">Cant.</th>")
                .append("<th align=\"right\" style=\"padding:10px 8px;font-size:12px;color:#6b7280;\">Precio</th>")
                .append("<th align=\"right\" style=\"padding:10px 8px;font-size:12px;color:#6b7280;\">Subtotal</th>")
                .append("</tr></thead><tbody>");
        for (OrderItem item : items) {
            String name = HtmlUtils.htmlEscape(item.getProductName());
            if (item.getVariantName() != null && !item.getVariantName().isBlank()) {
                name += " <span style=\"color:#6b7280;\">(" 
                        + HtmlUtils.htmlEscape(item.getVariantName()) + ")</span>";
            }
            html.append("<tr style=\"border-top:1px solid #e5e7eb;\">")
                    .append("<td style=\"padding:10px 8px;\">").append(name).append("</td>")
                    .append("<td align=\"center\" style=\"padding:10px 8px;\">").append(item.getQuantity()).append("</td>")
                    .append("<td align=\"right\" style=\"padding:10px 8px;\">").append(money(item.getUnitPrice())).append("</td>")
                    .append("<td align=\"right\" style=\"padding:10px 8px;\">").append(money(item.getSubtotal())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    /** Formatea una dirección en una línea. */
    private String address(Order order) {
        return String.join(", ",
                order.getAddressStreet(),
                order.getAddressCity(),
                order.getAddressPostalCode(),
                order.getAddressCountry());
    }

    /** Calcula el IVA repartido en la parte del pedido. */
    protected double tax(OrderSubOrder subOrder) {
        return Math.round((nz(subOrder.getTotal()) - nz(subOrder.getSubtotal())
                - nz(subOrder.getShipping())) * 100.0) / 100.0;
    }

    /** Formatea un importe en pesos colombianos sin decimales. */
    protected String money(Double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        format.setMaximumFractionDigits(0);
        return format.format(value == null ? 0.0 : value).replace('\u00A0', ' ');
    }

    /** Devuelve 0 si el importe es nulo. */
    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }

    /** Formatea una fecha en la zona horaria de Colombia. */
    private String formatDate(Instant instant) {
        return instant == null ? "" : DATE_FORMAT.format(instant.atZone(COLOMBIA));
    }

    /** Traduce el estado a español. */
    protected String statusLabel(OrderStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case PAID -> "Pagado";
            case PREPARED -> "Preparado";
            case SHIPPED -> "Enviado";
            case DELIVERED -> "Entregado";
            case CANCELLED -> "Cancelado";
        };
    }

    /** Comprueba que el correo de la tienda sea válido. */
    private boolean isValidEmail(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            new jakarta.mail.internet.InternetAddress(value).validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
