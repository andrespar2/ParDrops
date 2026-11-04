package com.pardorps.pardrops.payment;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.WompiEvent;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.WompiEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Verifica y procesa los eventos de Wompi.
 *
 * <p>Nunca confía en el cuerpo del evento por sí solo: valida el checksum con
 * el secreto de eventos y, para los estados finales, consulta la transacción
 * directamente a la API de Wompi con la llave privada.</p>
 */
@Service
public class WompiPaymentService {

    private static final Logger log = LoggerFactory.getLogger(WompiPaymentService.class);

    private static final String EVENT_TRANSACTION_UPDATED = "transaction.updated";

    private final WompiProperties properties;
    private final WompiSignatureService signatureService;
    private final WompiClient wompiClient;
    private final PaymentApplicationService applicationService;
    private final OrderRepository orderRepository;
    private final WompiEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * @param properties         configuración de Wompi.
     * @param signatureService   cálculo de firmas.
     * @param wompiClient        consulta de transacciones.
     * @param applicationService aplicación del pago al pedido.
     * @param orderRepository    acceso a pedidos.
     * @param eventRepository    registro de eventos.
     * @param objectMapper       lector de JSON.
     */
    public WompiPaymentService(
            WompiProperties properties,
            WompiSignatureService signatureService,
            WompiClient wompiClient,
            PaymentApplicationService applicationService,
            OrderRepository orderRepository,
            WompiEventRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.signatureService = signatureService;
        this.wompiClient = wompiClient;
        this.applicationService = applicationService;
        this.orderRepository = orderRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Procesa una notificación de Wompi.
     *
     * @param rawBody        cuerpo JSON recibido.
     * @param headerChecksum checksum de la cabecera {@code X-Event-Checksum}.
     * @return un mensaje operativo (nunca contiene secretos).
     */
    public String handleEvent(String rawBody, String headerChecksum) {
        if (properties.eventsSecret().isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El secreto de eventos de Wompi no está configurado");
        }

        JsonNode event = readTree(rawBody);
        String eventType = text(event, "event");
        if (!EVENT_TRANSACTION_UPDATED.equals(eventType)) {
            log.info("Evento de Wompi ignorado: {}", eventType);
            return "Evento ignorado";
        }

        JsonNode signature = event.path("signature");
        JsonNode propertiesNode = signature.path("properties");
        if (!propertiesNode.isArray() || propertiesNode.isEmpty()) {
            throw ApiException.badRequest("El evento no incluye las propiedades de la firma");
        }

        List<String> propertiesList = new ArrayList<>();
        propertiesNode.forEach(node -> propertiesList.add(node.asText()));

        long timestamp = event.path("timestamp").asLong(0L);
        String receivedChecksum = firstNonBlank(headerChecksum, text(signature, "checksum"));
        String expectedChecksum = signatureService.eventChecksum(
                event.path("data"), propertiesList, timestamp, properties.eventsSecret());

        String transactionId = signatureService.readPath(event.path("data"), "transaction.id");
        String reference = signatureService.readPath(event.path("data"), "transaction.reference");

        if (!signatureService.matches(expectedChecksum, receivedChecksum)) {
            recordRejected(transactionId, reference, "Checksum inválido");
            throw ApiException.badRequest("La firma del evento no es válida");
        }

        String environment = text(event, "environment");
        if (!properties.activeEnvironment().equalsIgnoreCase(environment)) {
            recordRejected(transactionId, reference, "Ambiente distinto al configurado");
            throw ApiException.badRequest("El evento pertenece a otro ambiente de Wompi");
        }

        if (transactionId.isBlank() || reference.isBlank()) {
            throw ApiException.badRequest("El evento no identifica la transacción o la referencia");
        }

        Order order = orderRepository.findByPaymentReference(reference).orElse(null);
        if (order == null) {
            log.warn("Evento de Wompi para una referencia desconocida: {}", reference);
            return "Referencia no registrada";
        }

        String eventStatus = signatureService.readPath(event.path("data"), "transaction.status");
        WompiTransaction transaction = fetchVerifiedOrFallback(transactionId, event, eventStatus);

        PaymentApplicationResult result = applicationService.apply(
                order.getId(), transaction, "webhook");

        recordProcessed(transactionId, reference, transaction, true, result.message());
        log.info("Evento de Wompi procesado para el pedido {}: {}", order.getId(), result.message());
        return result.message();
    }

    /**
     * Consulta una transacción concreta y la aplica si corresponde.
     *
     * <p>Se usa desde la pantalla de resultado: el identificador que Wompi
     * añade a la URL solo sirve como clave de consulta; la decisión se toma
     * con la respuesta autenticada de la API.</p>
     *
     * @param orderId       pedido.
     * @param transactionId identificador de la transacción.
     * @return el resultado, o {@code null} si no se pudo verificar.
     */
    public PaymentApplicationResult verifyAndApply(Long orderId, String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return null;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return null;
        }
        if (PaymentStatus.APPROVED.equals(order.getPaymentStatus())) {
            return new PaymentApplicationResult(
                    PaymentStatus.APPROVED, false, true, false, "La compra ya estaba confirmada");
        }

        WompiTransaction transaction;
        try {
            transaction = wompiClient.fetchTransaction(transactionId);
        } catch (WompiClientException exception) {
            log.warn("No se pudo verificar la transacción {}: {}",
                    transactionId, exception.getMessage());
            return null;
        }

        if (transaction.reference() == null
                || !transaction.reference().equals(order.getPaymentReference())) {
            log.warn("La transacción {} no corresponde al pedido {}", transactionId, orderId);
            return null;
        }

        PaymentApplicationResult result = applicationService.apply(
                orderId, transaction, "consulta");
        recordProcessed(transaction.id(), transaction.reference(), transaction, true, result.message());
        return result;
    }

    /**
     * Consulta la transacción en Wompi; si no es posible, usa los datos
     * firmados del evento únicamente para registros no aprobados.
     */
    private WompiTransaction fetchVerifiedOrFallback(
            String transactionId,
            JsonNode event,
            String eventStatus
    ) {
        String normalized = eventStatus == null
                ? ""
                : eventStatus.trim().toUpperCase(Locale.ROOT);

        if (properties.isVerificationConfigured()) {
            try {
                return wompiClient.fetchTransaction(transactionId);
            } catch (WompiClientException exception) {
                log.warn("Wompi no respondió al verificar {}: {}", transactionId,
                        exception.getMessage());
                if ("APPROVED".equals(normalized)) {
                    // Un evento aprobado debe confirmarse con la API antes de
                    // acreditar dinero; se responde error para que Wompi reintente.
                    throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                            "No se pudo verificar la transacción con Wompi");
                }
            }
        } else if ("APPROVED".equals(normalized)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La llave privada de Wompi no está configurada");
        }

        JsonNode transactionNode = event.path("data").path("transaction");
        return new WompiTransaction(
                text(transactionNode, "id"),
                text(transactionNode, "reference"),
                normalized,
                transactionNode.path("amount_in_cents").asLong(0L),
                text(transactionNode, "currency"),
                text(transactionNode, "payment_method_type"),
                text(transactionNode, "customer_email"),
                text(transactionNode, "status_message"),
                null
        );
    }

    /** Registra el evento procesado (idempotente por transacción). */
    private void recordProcessed(
            String transactionId,
            String reference,
            WompiTransaction transaction,
            boolean checksumVerified,
            String message
    ) {
        try {
            WompiEvent event = eventRepository.findByTransactionId(transactionId)
                    .orElseGet(WompiEvent::new);
            event.setTransactionId(transactionId);
            event.setReference(reference);
            event.setStatus(transaction.status());
            event.setEnvironment(properties.activeEnvironment());
            event.setChecksumVerified(checksumVerified);
            event.setProcessed(true);
            event.setResultMessage(limit(message));
            event.setProcessedAt(Instant.now());
            eventRepository.save(event);
        } catch (RuntimeException exception) {
            // Un fallo al registrar el evento no debe impedir la confirmación
            // ya aplicada; Wompi reintentará y la idempotencia la protege.
            log.warn("No se pudo registrar el evento de Wompi {}: {}",
                    transactionId, exception.getMessage());
        }
    }

    /** Registra un evento rechazado para auditoría. */
    private void recordRejected(String transactionId, String reference, String message) {
        if (transactionId == null || transactionId.isBlank()) {
            log.warn("Evento de Wompi rechazado: {}", message);
            return;
        }
        try {
            WompiEvent event = eventRepository.findByTransactionId(transactionId)
                    .orElseGet(WompiEvent::new);
            event.setTransactionId(transactionId);
            event.setReference(reference);
            event.setChecksumVerified(false);
            event.setProcessed(false);
            event.setResultMessage(limit(message));
            eventRepository.save(event);
        } catch (RuntimeException exception) {
            log.warn("No se pudo registrar el evento rechazado: {}", exception.getMessage());
        }
    }

    private JsonNode readTree(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception exception) {
            throw ApiException.badRequest("El cuerpo del evento no es JSON válido");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String limit(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 300 ? message.substring(0, 300) : message;
    }
}
