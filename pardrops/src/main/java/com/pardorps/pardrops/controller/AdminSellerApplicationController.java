package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerApplicationResponse;
import com.pardorps.pardrops.dto.SellerApplicationReviewRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.SellerApplicationService;
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
 * API de administración de las solicitudes para convertirse en vendedor.
 *
 * <p>Solo accesible con rol ADMIN: la configuración de seguridad bloquea el
 * acceso de cualquier otro rol antes de llegar al controlador.</p>
 */
@RestController
@RequestMapping("/api/admin/seller-applications")
public class AdminSellerApplicationController {

    private final SellerApplicationService sellerApplicationService;
    private final AuthService authService;

    /**
     * @param sellerApplicationService servicio de solicitudes de vendedor.
     * @param authService               servicio para obtener el usuario autenticado.
     */
    public AdminSellerApplicationController(
            SellerApplicationService sellerApplicationService,
            AuthService authService
    ) {
        this.sellerApplicationService = sellerApplicationService;
        this.authService = authService;
    }

    /**
     * Lista todas las solicitudes registradas.
     *
     * @return 200 con el listado de solicitudes.
     */
    @GetMapping
    public ResponseEntity<List<SellerApplicationResponse>> list() {
        return ResponseEntity.ok(sellerApplicationService.findAll());
    }

    /**
     * Lista las solicitudes pendientes de revisión.
     *
     * @return 200 con el listado de solicitudes pendientes.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<SellerApplicationResponse>> pending() {
        return ResponseEntity.ok(sellerApplicationService.findPending());
    }

    /**
     * Aprueba o rechaza una solicitud.
     *
     * @param id      identificador de la solicitud.
     * @param request decisión del administrador.
     * @return 200 con la solicitud revisada.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SellerApplicationResponse> review(
            @PathVariable Long id,
            @Valid @RequestBody SellerApplicationReviewRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(sellerApplicationService.review(id, admin, request));
    }
}
