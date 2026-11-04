package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminPaymentsResponse;
import com.pardorps.pardrops.service.AdminPaymentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de transacciones de Wompi para administración.
 *
 * <p>Solo accesible con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentsController {

    private final AdminPaymentsService service;

    /**
     * @param service consulta de pagos.
     */
    public AdminPaymentsController(AdminPaymentsService service) {
        this.service = service;
    }

    /**
     * Devuelve las transacciones y los totales recaudados.
     *
     * @return 200 con el resumen.
     */
    @GetMapping
    public ResponseEntity<AdminPaymentsResponse> summary() {
        return ResponseEntity.ok(service.summary());
    }
}
