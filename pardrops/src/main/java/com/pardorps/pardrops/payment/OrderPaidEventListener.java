package com.pardorps.pardrops.payment;

import com.pardorps.pardrops.service.PaymentNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara los correos de la compra confirmada después del commit.
 *
 * <p>Así, si el proveedor de correo falla, la transacción financiera ya está
 * guardada y la compra no se revierte.</p>
 */
@Component
public class OrderPaidEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidEventListener.class);

    private final PaymentNotificationService notificationService;

    /**
     * @param notificationService envío de los correos personalizados.
     */
    public OrderPaidEventListener(PaymentNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Envía los correos cuando la compra ya está confirmada en la base de datos.
     *
     * @param event pedido confirmado.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        try {
            notificationService.sendAllEmails(event.orderId());
        } catch (RuntimeException exception) {
            log.error("No se pudieron enviar los correos del pedido {}: {}",
                    event.orderId(), exception.getMessage());
        }
    }
}
