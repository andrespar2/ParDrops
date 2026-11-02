package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.WithdrawalMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de retiro del vendedor.
 *
 * @param amount        valor solicitado.
 * @param method        método de retiro.
 * @param breBKey       llave Bre-B (si aplica).
 * @param bankName      banco (si aplica).
 * @param accountType   tipo de cuenta (si aplica).
 * @param accountNumber número de cuenta (si aplica).
 * @param accountHolder titular (si aplica).
 * @param notes         observaciones.
 */
public record CreateWithdrawalRequest(
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "1", message = "El valor debe ser mayor que cero")
        Double amount,

        @NotNull(message = "El método de retiro es obligatorio")
        WithdrawalMethod method,

        @Size(max = 120) String breBKey,
        @Size(max = 120) String bankName,
        @Size(max = 40) String accountType,
        @Size(max = 60) String accountNumber,
        @Size(max = 120) String accountHolder,
        @Size(max = 500) String notes
) {
}
