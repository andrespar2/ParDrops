package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerOrderResponse;
import com.pardorps.pardrops.dto.UpdateOrderStatusRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.SellerOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de los pedidos de un vendedor.
 *
 * <p>El vendedor solo ve y gestiona las partes de pedido que le pertenecen:
 * la comprobación de propiedad la hace {@link SellerOrderService}, no la
 * interfaz. Solo accesible con rol SELLER.</p>
 */
@RestController
@RequestMapping("/api/seller/orders")
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;
    private final AuthService authService;

    /**
     * @param sellerOrderService servicio de pedidos del vendedor.
     * @param authService        servicio para obtener el usuario autenticado.
     */
    public SellerOrderController(SellerOrderService sellerOrderService, AuthService authService) {
        this.sellerOrderService = sellerOrderService;
        this.authService = authService;
    }

    /**
     * Lista las partes de pedido del vendedor autenticado.
     *
     * @return 200 con la lista de partes del vendedor.
     */
    @GetMapping
    public ResponseEntity<List<SellerOrderResponse>> list() {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(sellerOrderService.findMine(seller));
    }

    /**
     * Avanza el estado de una parte de pedido del vendedor.
     *
     * @param id      identificador de la parte de pedido.
     * @param request nuevo estado solicitado.
     * @return 200 con la parte de pedido actualizada.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SellerOrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(sellerOrderService.updateStatus(seller, id, request.status()));
    }
}
