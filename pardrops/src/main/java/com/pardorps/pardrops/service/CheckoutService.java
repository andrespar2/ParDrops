package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CheckoutResponse;
import com.pardorps.pardrops.dto.CheckoutStatusResponse;
import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.payment.WompiPaymentService;
import com.pardorps.pardrops.payment.WompiProperties;
import com.pardorps.pardrops.payment.WompiSignatureService;
import com.pardorps.pardrops.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Checkout con Wompi.
 *
 * <p>Crea el pedido pendiente con precios recalculados desde la base de datos,
 * aplica el cargo fijo de servicio una sola vez, genera una referencia única y
 * firma la integridad con el secreto del backend.</p>
 */
@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final WompiProperties properties;
    private final WompiSignatureService signatureService;
    private final WompiPaymentService paymentService;

    /**
     * @param orderService      creación del pedido pendiente.
     * @param orderRepository   acceso a los pedidos.
     * @param properties        configuración de Wompi.
     * @param signatureService  firma de integridad.
     * @param paymentService    verificación de transacciones.
     */
    public CheckoutService(
            OrderService orderService,
            OrderRepository orderRepository,
            WompiProperties properties,
            WompiSignatureService signatureService,
            WompiPaymentService paymentService
    ) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.signatureService = signatureService;
        this.paymentService = paymentService;
    }

    /**
     * Prepara un checkout nuevo.
     *
     * @param request datos del comprador y del carrito.
     * @param user    usuario autenticado o {@code null}.
     * @return los datos que necesita el widget de Wompi.
     */
    @Transactional
    public CheckoutResponse createCheckout(CreateOrderRequest request, User user) {
        requireCheckoutConfigured();

        double fee = round(properties.getFixedFeeCop());
        Order order = orderService.persistPendingOrder(request, user);
        order.setServiceFee(fee);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentEnvironment(properties.activeEnvironment());
        order.setPaymentReference(generateReference(order.getId()));
        order.setTotal(round(nz(order.getSubtotal()) + nz(order.getShipping()) + fee));

        Order saved = orderRepository.save(order);
        log.info("Checkout creado para el pedido {} con referencia {}", saved.getId(),
                saved.getPaymentReference());
        return toCheckoutResponse(saved);
    }

    /**
     * Regenera la referencia para reintentar un pago no aprobado.
     *
     * @param reference referencia actual del pedido.
     * @return los datos del nuevo intento de pago.
     */
    @Transactional
    public CheckoutResponse retryCheckout(String reference) {
        requireCheckoutConfigured();
        Order order = orderRepository.findByPaymentReference(reference)
                .orElseThrow(() -> ApiException.notFound("No existe un pago con esa referencia"));

        if (PaymentStatus.APPROVED.equals(order.getPaymentStatus())) {
            throw ApiException.conflict("La compra ya está pagada; no es necesario reintentar");
        }
        if (order.getPaymentStatus() == null
                || !order.getPaymentStatus().isFinal()) {
            throw ApiException.conflict("El pago sigue en proceso; espera la confirmación");
        }

        order.setPaymentReference(generateReference(order.getId()));
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setWompiTransactionId(null);
        order.setPaymentMethod(null);
        orderRepository.save(order);
        return toCheckoutResponse(order);
    }

    /**
     * Consulta el estado de un checkout.
     *
     * <p>Si el navegador llega con el identificador de la transacción que Wompi
     * añade a la URL de retorno, se usa solo como clave para consultar la API:
     * la compra se confirma con la respuesta autenticada de Wompi.</p>
     *
     * @param reference     referencia del pago.
     * @param transactionId identificador informado por Wompi (opcional).
     * @return el estado actual del pedido y del pago.
     */
    @Transactional
    public CheckoutStatusResponse getStatus(String reference, String transactionId) {
        Order order = orderRepository.findByPaymentReference(reference)
                .orElseThrow(() -> ApiException.notFound("No existe un pago con esa referencia"));

        if (!PaymentStatus.APPROVED.equals(order.getPaymentStatus())
                && transactionId != null && !transactionId.isBlank()) {
            paymentService.verifyAndApply(order.getId(), transactionId);
            order = orderRepository.findById(order.getId()).orElse(order);
        }

        PaymentStatus status = order.getPaymentStatus();
        boolean approved = PaymentStatus.APPROVED.equals(status);
        return new CheckoutStatusResponse(
                order.getPaymentReference(),
                orderService.toPublicResponse(order),
                status,
                approved,
                status == PaymentStatus.DECLINED
                        || status == PaymentStatus.VOIDED
                        || status == PaymentStatus.ERROR,
                statusMessage(status),
                Instant.now()
        );
    }

    /**
     * Devuelve el estado del pedido a partir de su referencia (sin verificar).
     *
     * @param reference referencia del pago.
     * @return el estado actual.
     */
    @Transactional(readOnly = true)
    public CheckoutStatusResponse getStatus(String reference) {
        return getStatus(reference, null);
    }

    private CheckoutResponse toCheckoutResponse(Order order) {
        long amountInCents = Math.round(nz(order.getTotal()) * 100.0);
        String signature = signatureService.integritySignature(
                order.getPaymentReference(),
                amountInCents,
                properties.getCurrency(),
                properties.integritySecret());

        return new CheckoutResponse(
                order.getId(),
                order.getPaymentReference(),
                amountInCents,
                properties.getCurrency(),
                properties.publicKey(),
                signature,
                properties.resultUrl(order.getPaymentReference()),
                properties.activeEnvironment(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getSubtotal(),
                order.getShipping(),
                order.getServiceFee(),
                order.getTotal(),
                orderService.toResponse(order).items()
        );
    }

    private void requireCheckoutConfigured() {
        if (!properties.isCheckoutConfigured()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "La pasarela de pagos no está configurada en este momento");
        }
    }

    private String generateReference(Long orderId) {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return "PD-" + orderId + "-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private String statusMessage(PaymentStatus status) {
        if (status == null) {
            return "Este pedido no tiene un pago asociado.";
        }
        return switch (status) {
            case APPROVED -> "Tu pago fue confirmado y tu pedido está registrado.";
            case PENDING -> "Estamos verificando tu pago. Esto puede tardar unos instantes.";
            case DECLINED -> "El pago no fue aprobado. Puedes intentarlo de nuevo.";
            case VOIDED -> "La transacción fue anulada. Puedes intentarlo de nuevo.";
            case ERROR -> "Ocurrió un error al procesar el pago. Puedes intentarlo de nuevo.";
        };
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double nz(Double value) {
        return value == null ? 0.0 : value;
    }
}
