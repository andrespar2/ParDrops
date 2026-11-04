package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.PaymentEmailKind;
import com.pardorps.pardrops.model.PaymentEmailLog;
import com.pardorps.pardrops.model.PaymentEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio del registro de correos de confirmación de compra.
 */
public interface PaymentEmailLogRepository extends JpaRepository<PaymentEmailLog, Long> {

    /**
     * Busca el registro de un correo concreto.
     *
     * @param orderId   pedido.
     * @param kind      tipo de correo.
     * @param recipient correo del destinatario.
     * @return el registro, si existe.
     */
    Optional<PaymentEmailLog> findByOrderIdAndKindAndRecipient(
            Long orderId, PaymentEmailKind kind, String recipient);

    /**
     * Devuelve los envíos registrados de un pedido.
     *
     * @param orderId pedido.
     * @return registros de envío.
     */
    List<PaymentEmailLog> findByOrderId(Long orderId);

    /**
     * Cuenta los envíos correctos de un pedido.
     *
     * @param orderId pedido.
     * @param status  estado.
     * @return número de registros en ese estado.
     */
    long countByOrderIdAndStatus(Long orderId, PaymentEmailStatus status);
}
