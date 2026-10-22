package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerApplicationRequest;
import com.pardorps.pardrops.dto.SellerApplicationResponse;
import com.pardorps.pardrops.dto.SellerApplicationReviewRequest;
import com.pardorps.pardrops.model.SellerApplicationStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de solicitudes para convertirse en vendedor.
 *
 * <p>El envío y la consulta de la solicitud propia son operaciones del
 * cliente; el listado de todas las solicitudes y la revisión de una decisión
 * son operaciones del administrador, y la configuración de seguridad se
 * encarga de separarlas: el cliente recibe un 403 si intenta listar o revisar
 * solicitudes.</p>
 */
@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;
    private final AuthService authService;

    /**
     * @param sellerApplicationService servicio de solicitudes de vendedor.
     * @param authService               servicio para obtener el usuario autenticado.
     */
    public SellerApplicationController(
            SellerApplicationService sellerApplicationService,
            AuthService authService
    ) {
        this.sellerApplicationService = sellerApplicationService;
        this.authService = authService;
    }

    /**
     * Envía la solicitud para convertirse en vendedor.
     *
     * @param request datos del negocio.
     * @return 201 con la solicitud registrada.
     */
    @PostMapping
    public ResponseEntity<SellerApplicationResponse> submit(
            @Valid @RequestBody SellerApplicationRequest request
    ) {
        User user = authService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerApplicationService.submit(user, request));
    }

    /**
     * Devuelve la última solicitud enviada por el usuario autenticado.
     *
     * @return 200 con la solicitud, o 200 con cuerpo vacío si no hay ninguna.
     */
    @GetMapping("/me")
    public ResponseEntity<SellerApplicationResponse> mine() {
        return ResponseEntity.ok(
                sellerApplicationService.findMine(authService.getCurrentUser()));
    }

    /**
     * Lista todas las solicitudes registradas (solo administrador).
     *
     * @param status filtro opcional por estado.
     * @return 200 con el listado de solicitudes.
     */
    @GetMapping
    public ResponseEntity<List<SellerApplicationResponse>> list(
            @RequestParam(required = false) SellerApplicationStatus status
    ) {
        return ResponseEntity.ok(sellerApplicationService.findAll(status));
    }

    /**
     * Aprueba o rechaza una solicitud de vendedor (solo administrador).
     *
     * @param id      identificador de la solicitud.
     * @param request decisión del administrador.
     * @return 200 con la solicitud revisada.
     */
    @PatchMapping("/{id}/decision")
    public ResponseEntity<SellerApplicationResponse> decide(
            @PathVariable Long id,
            @Valid @RequestBody SellerApplicationReviewRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(sellerApplicationService.review(id, admin, request));
    }
}
