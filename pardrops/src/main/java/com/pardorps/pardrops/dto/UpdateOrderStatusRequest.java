package com.pardorps.pardrops.dto;

import com.pardorps.pardrops.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para cambiar el estado de un pedido.
 *
 * <p>Lo usa el panel de administración para avanzar un pedido por su flujo de
 * preparación ({@code PREPARED}, {@code SHIPPED}, {@code DELIVERED}…).</p>
 *
 * @param status nuevo estado que se quiere aplicar al pedido.
 */
public record UpdateOrderStatusRequest(

        @NotNull(message = "El estado es obligatorio")
        OrderStatus status
) {
}
