package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.WithdrawalMethod;
import com.pardorps.pardrops.model.WithdrawalStatus;

import java.time.Instant;

/**
 * Solicitud de retiro expuesta al vendedor y a administración.
 *
 * @param id           identificador.
 * @param sellerName   nombre del vendedor.
 * @param amount       valor solicitado.
 * @param fee          comisión de ParDrops.
 * @param netAmount    valor neto a transferir.
 * @param method       método.
 * @param breBKey      llave Bre-B enmascarada.
 * @param bankName     banco.
 * @param accountType  tipo de cuenta.
 * @param accountNumber cuenta enmascarada.
 * @param accountHolder titular.
 * @param status       estado.
 * @param notes        observaciones.
 * @param reference    referencia de la transferencia.
 * @param createdAt    fecha de solicitud.
 * @param resolvedAt   fecha de resolución.
 */
public record WithdrawalResponse(
        Long id,
        String sellerName,
        Double amount,
        Double fee,
        Double netAmount,
        WithdrawalMethod method,
        String breBKey,
        String bankName,
        String accountType,
        String accountNumber,
        String accountHolder,
        WithdrawalStatus status,
        String notes,
        String reference,
        Instant createdAt,
        Instant resolvedAt
) {
}
