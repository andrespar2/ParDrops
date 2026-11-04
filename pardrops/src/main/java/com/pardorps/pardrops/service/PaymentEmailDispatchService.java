package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.model.PaymentEmailKind;
import com.pardorps.pardrops.model.PaymentEmailLog;
import com.pardorps.pardrops.model.PaymentEmailStatus;
import com.pardorps.pardrops.repository.PaymentEmailLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Envía cada correo de confirmación una sola vez y registra su resultado.
 *
 * <p>La base de datos actúa como memoria entre reinicios: un mismo pedido,
 * tipo y destinatario no vuelve a recibir el correo si el envío ya consta como
 * correcto. Los envíos fallidos admiten un número limitado de reintentos.</p>
 */
@Service
public class PaymentEmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(PaymentEmailDispatchService.class);

    /** Intentos máximos por destinatario y tipo de correo. */
    private static final int MAX_ATTEMPTS = 3;

    private final PaymentEmailLogRepository logRepository;
    private final EmailService emailService;

    /**
     * @param logRepository registro de envíos.
     * @param emailService  entrega de correos con reintentos internos.
     */
    public PaymentEmailDispatchService(
            PaymentEmailLogRepository logRepository,
            EmailService emailService
    ) {
        this.logRepository = logRepository;
        this.emailService = emailService;
    }

    /**
     * Envía el correo si no se había entregado ya.
     *
     * @param orderId   pedido.
     * @param kind      tipo de correo.
     * @param recipient destinatario.
     * @param email     correo renderizado.
     * @return {@code true} si el envío quedó registrado como correcto.
     */
    public boolean sendOnce(
            Long orderId,
            PaymentEmailKind kind,
            String recipient,
            RenderedEmail email
    ) {
        if (recipient == null || recipient.isBlank() || email == null) {
            return false;
        }
        String to = recipient.trim();

        PaymentEmailLog entry = logRepository
                .findByOrderIdAndKindAndRecipient(orderId, kind, to)
                .orElse(null);

        if (entry != null && entry.getStatus() == PaymentEmailStatus.SENT) {
            log.debug("Correo {} ya enviado a {} para el pedido {}", kind, to, orderId);
            return true;
        }
        if (entry != null && entry.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Correo {} a {} para el pedido {} agotó los reintentos", kind, to, orderId);
            return false;
        }

        if (entry == null) {
            entry = new PaymentEmailLog();
            entry.setOrderId(orderId);
            entry.setKind(kind);
            entry.setRecipient(to);
            entry.setAttempts(0);
            entry.setStatus(PaymentEmailStatus.FAILED);
        }
        entry.setAttempts(entry.getAttempts() + 1);

        boolean sent;
        try {
            sent = emailService.send(to, email.subject(), email.bodyHtml());
        } catch (RuntimeException exception) {
            log.warn("Error enviando el correo {} a {}: {}", kind, to, exception.getMessage());
            sent = false;
        }

        if (sent) {
            entry.setStatus(PaymentEmailStatus.SENT);
            entry.setSentAt(Instant.now());
            entry.setLastError(null);
        } else {
            entry.setStatus(PaymentEmailStatus.FAILED);
            entry.setLastError("No se pudo entregar el correo");
        }
        logRepository.save(entry);
        return sent;
    }
}
