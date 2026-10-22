package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminUserResponse;
import com.pardorps.pardrops.dto.UserStatusRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AdminUserService;
import com.pardorps.pardrops.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración de cuentas de usuario.
 *
 * <p>Permite consultar y filtrar las cuentas de la plataforma y cambiar su
 * estado (activa o suspendida). Solo accesible con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuthService authService;

    /**
     * @param adminUserService servicio de administración de cuentas.
     * @param authService      servicio para obtener el usuario autenticado.
     */
    public AdminUserController(AdminUserService adminUserService, AuthService authService) {
        this.adminUserService = adminUserService;
        this.authService = authService;
    }

    /**
     * Lista las cuentas de la plataforma, opcionalmente filtradas.
     *
     * @param search texto de búsqueda por correo, nombre o teléfono (opcional).
     * @return 200 con el listado de cuentas.
     */
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(adminUserService.search(search));
    }

    /**
     * Suspende o reactiva el acceso de una cuenta.
     *
     * @param id      identificador de la cuenta.
     * @param request nuevo estado y motivo del cambio.
     * @return 200 con la cuenta actualizada.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(adminUserService.changeStatus(id, admin, request));
    }
}
