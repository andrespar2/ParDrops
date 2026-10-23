package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminDashboardResponse;
import com.pardorps.pardrops.service.AdminDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API del resumen general del panel de administración.
 *
 * <p>Devuelve los indicadores de la plataforma (cuentas, catálogo, pedidos y
 * avisos pendientes) junto con los últimos pedidos y las tiendas con más
 * ventas. Solo accesible con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * @param adminDashboardService servicio con los indicadores del panel.
     */
    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /**
     * Devuelve el resumen general de la plataforma.
     *
     * @return 200 con los indicadores del panel.
     */
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> summary() {
        return ResponseEntity.ok(adminDashboardService.summary());
    }
}
