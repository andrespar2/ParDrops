package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.dto.UpdateOrderStatusRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de pedidos.
 *
 * <p>Cualquiera puede registrar una compra: si hay sesión iniciada, el pedido
 * queda vinculado a la cuenta y aparece en su historial; si no, se registra
 * como compra de invitado sin asociarse a ningún usuario.</p>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    /**
     * @param orderService servicio de pedidos.
     * @param authService  servicio para obtener el usuario autenticado.
     */
    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    /**
     * Registra un pedido (cliente autenticado o invitado).
     *
     * @param request datos del checkout.
     * @return 201 con el pedido creado.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        User user = authService.getCurrentUserOrNull();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(request, user));
    }

    /**
     * Devuelve el historial de pedidos del usuario autenticado.
     *
     * @return 200 con la lista de pedidos del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> myOrders() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(orderService.getOrdersForUser(user));
    }

    /**
     * Devuelve todos los pedidos (solo administrador).
     *
     * @return 200 con la lista completa de pedidos.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> allOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Cambia el estado de un pedido (solo administrador).
     *
     * @param id      identificador del pedido.
     * @param request nuevo estado solicitado.
     * @return 200 con el pedido actualizado.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.status()));
    }
}
