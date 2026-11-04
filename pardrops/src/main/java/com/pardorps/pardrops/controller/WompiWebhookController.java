package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.payment.WompiPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook público de Wompi.
 *
 * <p>Recibe los eventos {@code transaction.updated}. La respuesta 200 confirma
 * a Wompi que el evento se procesó; cualquier otro estado provoca un reintento
 * automático.</p>
 */
@RestController
@RequestMapping("/api/webhooks/wompi")
public class WompiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WompiWebhookController.class);

    private final WompiPaymentService paymentService;

    /**
     * @param paymentService verificación y aplicación del evento.
     */
    public WompiWebhookController(WompiPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Procesa un evento de Wompi.
     *
     * @param rawBody        cuerpo original del evento.
     * @param checksum       checksum de la cabecera {@code X-Event-Checksum}.
     * @return 200 con un mensaje operativo.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum
    ) {
        String result = paymentService.handleEvent(rawBody, checksum);
        log.info("Webhook de Wompi procesado: {}", result);
        return ResponseEntity.ok(Map.of("message", result));
    }
}
