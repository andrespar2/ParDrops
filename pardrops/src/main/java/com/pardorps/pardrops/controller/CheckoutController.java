package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CheckoutResponse;
import com.pardorps.pardrops.dto.CheckoutStatusResponse;
import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública del checkout con Wompi.
 *
 * <p>Crea el pedido pendiente y devuelve los datos del widget. La pantalla de
 * resultado consulta aquí el estado real del pago; la compra solo se considera
 * realizada cuando el backend verificó la transacción con Wompi.</p>
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final AuthService authService;

    /**
     * @param checkoutService servicio de checkout.
     * @param authService     resolución del usuario autenticado (opcional).
     */
    public CheckoutController(CheckoutService checkoutService, AuthService authService) {
        this.checkoutService = checkoutService;
        this.authService = authService;
    }

    /**
     * Crea un pedido pendiente de pago y prepara el widget de Wompi.
     *
     * @param request datos del comprador y del carrito.
     * @return 201 con los datos del checkout.
     */
    @PostMapping
    public ResponseEntity<CheckoutResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        User user = authService.getCurrentUserOrNull();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.createCheckout(request, user));
    }

    /**
     * Consulta el estado de un pago.
     *
     * @param reference     referencia del pago.
     * @param transactionId identificador de Wompi añadido a la URL de retorno.
     * @return 200 con el estado del pedido y del pago.
     */
    @GetMapping("/{reference}")
    public ResponseEntity<CheckoutStatusResponse> status(
            @PathVariable String reference,
            @RequestParam(required = false) String transactionId
    ) {
        return ResponseEntity.ok(checkoutService.getStatus(reference, transactionId));
    }

    /**
     * Genera una nueva referencia para reintentar un pago no aprobado.
     *
     * @param reference referencia actual.
     * @return 200 con los datos del nuevo intento.
     */
    @PostMapping("/{reference}/retry")
    public ResponseEntity<CheckoutResponse> retry(@PathVariable String reference) {
        return ResponseEntity.ok(checkoutService.retryCheckout(reference));
    }
}
