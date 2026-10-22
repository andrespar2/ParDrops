package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SellerApplicationRequest;
import com.pardorps.pardrops.dto.SellerApplicationResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.SellerApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de solicitudes para convertirse en vendedor.
 *
 * <p>El envío y la consulta de la solicitud propia son operaciones del
 * cliente; la revisión de las solicitudes se expone en
 * {@link AdminSellerApplicationController}.</p>
 */
@RestController
@RequestMapping("/api/seller/applications")
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
}
