package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerDashboardResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.SellerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del panel privado de cada vendedor.
 *
 * <p>Solo accesible con rol SELLER. El resumen se calcula siempre para el
 * vendedor autenticado, así que no existe forma de pedir el panel de otra
 * tienda.</p>
 */
@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerDashboardService sellerDashboardService;
    private final AuthService authService;

    /**
     * @param sellerDashboardService servicio de indicadores del vendedor.
     * @param authService            servicio para obtener el usuario autenticado.
     */
    public SellerController(
            SellerDashboardService sellerDashboardService,
            AuthService authService
    ) {
        this.sellerDashboardService = sellerDashboardService;
        this.authService = authService;
    }

    /**
     * Devuelve los indicadores del panel del vendedor autenticado.
     *
     * @return 200 con el resumen de ventas, pedidos, inventario e ingresos.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<SellerDashboardResponse> dashboard() {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(sellerDashboardService.loadSummary(seller));
    }
}
