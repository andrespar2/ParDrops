package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.mail.EmailTemplateRenderer;
import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.model.FundsStatus;
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
import java.util.Optional;

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
    private final String supportEmail;

    /**
     * @param templateService servicio de plantillas.
     * @param emailService    entrega de correos.
     * @param storeRepository acceso a la tienda del vendedor.
     * @param frontendUrl     URL base del frontend.
     * @param supportEmail    correo de contacto mostrado en los mensajes.
     */
    public OrderEmailService(
            EmailTemplateService templateService,
            EmailService emailService,
            StoreRepository storeRepository,
            @Value("${app.mail.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.mail.support-email:soporte@pardrops.com}") String supportEmail
    ) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.storeRepository = storeRepository;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.supportEmail = supportEmail;
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
     * Prepara el correo de compra confirmada para el comprador.
     *
     * @param order pedido aprobado.
     * @return el correo renderizado, o vacío si la plantilla está inactiva.
     */
    public Optional<RenderedEmail> renderBuyerPurchaseConfirmed(Order order) {
        Map<String, Object> model = buildModel(order, null);
        enrichPaymentModel(order, null, model);
        return templateService.renderIfActive(EmailTemplateCode.BUYER_PURCHASE_CONFIRMED, model);
    }

    /**
     * Prepara el correo de venta confirmada para un vendedor.
     *
     * @param order    pedido aprobado.
     * @param subOrder parte del vendedor.
     * @return el correo renderizado, o vacío si la plantilla está inactiva.
     */
    public Optional<RenderedEmail> renderSellerSaleConfirmed(Order order, OrderSubOrder subOrder) {
        Map<String, Object> model = buildModel(order, subOrder);
        enrichPaymentModel(order, subOrder, model);
        return templateService.renderIfActive(EmailTemplateCode.SELLER_SALE_CONFIRMED, model);
    }

    /**
     * Prepara el correo de venta confirmada para administración.
     *
     * @param order pedido aprobado.
     * @return el correo renderizado, o vacío si la plantilla está inactiva.
     */
    public Optional<RenderedEmail> renderAdminSaleConfirmed(Order order) {
        Map<String, Object> model = buildModel(order, null);
        enrichPaymentModel(order, null, model);
        return templateService.renderIfActive(EmailTemplateCode.ADMIN_SALE_CONFIRMED, model);
    }

    /**
     * Añade al modelo los datos propios de una compra confirmada.
     *
     * @param order    pedido aprobado.
     * @param subOrder parte del vendedor, o {@code null} para el pedido completo.
     * @param model    variables de la plantilla.
     */
    protected void enrichPaymentModel(Order order, OrderSubOrder subOrder, Map<String, Object> model) {
        Instant paidAt = order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt();
        double totalCommission = order.getSubOrders().stream()
                .mapToDouble(part -> part.getCommissionAmount() == null ? 0.0 : part.getCommissionAmount())
                .sum();

        model.put("paymentReference", order.getPaymentReference() == null ? "" : order.getPaymentReference());
        model.put("wompiTransactionId", order.getWompiTransactionId() == null ? "" : order.getWompiTransactionId());
        model.put("paymentDate", formatDate(paidAt));
        model.put("paymentMethod", paymentMethodLabel(order.getPaymentMethod()));
        model.put("paymentStatusLabel", "Aprobado");
        model.put("serviceFee", money(order.getServiceFee()));
        model.put("totalPaid", money(order.getTotal()));
        model.put("commission", money(subOrder == null
                ? totalCommission
                : nz(subOrder.getCommissionAmount())));
        model.put("sellerNet", money(subOrder == null ? 0.0 : nz(subOrder.getSellerNetAmount())));
        model.put("fundsStatusLabel", subOrder == null
                ? ""
                : fundsStatusLabel(subOrder.getFundsStatus()));
        model.put("sellersSummary", sellersSummary(order));
        model.put("distributionTable", new EmailTemplateRenderer.SafeHtml(distributionTable(order)));
        model.put("adminOrderUrl", frontendUrl + "/admin/pedidos");
        model.put("orderUrl", frontendUrl + "/cuenta");
        model.put("supportEmail", supportEmail);
    }

    /**
     * Construye la tabla de distribución de ingresos por vendedor.
     *
     * @param order pedido aprobado.
     * @return tabla HTML.
     */
    private String distributionTable(Order order) {
        StringBuilder html = new StringBuilder();
        html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"width:100%;border-collapse:collapse;font-size:13px;color:#374151;\">")
                .append("<thead><tr style=\"background:#f9fafb;\">")
                .append("<th align=\"left\" style=\"padding:8px;font-size:12px;color:#6b7280;\">Tienda</th>")
                .append("<th align=\"right\" style=\"padding:8px;font-size:12px;color:#6b7280;\">Productos</th>")
                .append("<th align=\"right\" style=\"padding:8px;font-size:12px;color:#6b7280;\">Comisión</th>")
                .append("<th align=\"right\" style=\"padding:8px;font-size:12px;color:#6b7280;\">Neto</th>")
                .append("</tr></thead><tbody>");
        for (OrderSubOrder part : order.getSubOrders()) {
            String store = part.getStoreName() == null ? "ParDrops" : part.getStoreName();
            html.append("<tr style=\"border-top:1px solid #e5e7eb;\">")
                    .append("<td style=\"padding:8px;\">").append(HtmlUtils.htmlEscape(store)).append("</td>")
                    .append("<td align=\"right\" style=\"padding:8px;\">").append(money(part.getSubtotal())).append("</td>")
                    .append("<td align=\"right\" style=\"padding:8px;\">").append(money(part.getCommissionAmount())).append("</td>")
                    .append("<td align=\"right\" style=\"padding:8px;\">").append(money(part.getSellerNetAmount())).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    /** Lista las tiendas participantes. */
    private String sellersSummary(Order order) {
        return order.getSubOrders().stream()
                .map(part -> part.getStoreName() == null ? "ParDrops" : part.getStoreName())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("ParDrops");
    }

    /** Traduce el estado de fondos a lenguaje natural. */
    protected String fundsStatusLabel(FundsStatus status) {
        if (status == null) {
            return "Pendiente";
        }
        return switch (status) {
            case HELD -> "Pendiente de liberación";
            case RELEASED -> "Disponible para retiro";
            case CANCELLED -> "Cancelado";
        };
    }

    /** Etiqueta legible del método de pago. */
    protected String paymentMethodLabel(String method) {
        if (method == null || method.isBlank()) {
            return "Wompi";
        }
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "CARD" -> "Tarjeta";
            case "NEQUI" -> "Nequi";
            case "PSE" -> "PSE";
            case "BANCOLOMBIA_TRANSFER" -> "Transferencia Bancolombia";
            case "BANCOLOMBIA_QR" -> "QR Bancolombia";
            case "BANCOLOMBIA_COLLECT" -> "Bancolombia Collect";
            default -> method;
        };
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
    public String sellerEmail(OrderSubOrder subOrder) {
        User seller = subOrder.getSeller();
        if (seller == null) {
            return null;
        }
        Store store = storeRepository.findBySellerId(seller.getId()).orElse(null);
        if (store != null && isValidEmail(store.getNotificationEmail())) {
            return store.getNotificationEmail().trim();
        }
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
