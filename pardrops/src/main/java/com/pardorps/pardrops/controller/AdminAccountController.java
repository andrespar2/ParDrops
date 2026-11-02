package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminAccountResponse;
import com.pardorps.pardrops.dto.AdminAccountStatusRequest;
import com.pardorps.pardrops.dto.CreateAdminRequest;
import com.pardorps.pardrops.dto.UpdateAdminLevelRequest;
import com.pardorps.pardrops.service.AdminAccountService;
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
 * API de cuentas administrativas.
 *
 * <p>Solo accesible para administradores; las reglas de quién puede crear o
 * modificar cuentas se validan en el servicio.</p>
 */
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    /**
     * @param adminAccountService servicio de cuentas administrativas.
     */
    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    /**
     * Lista las cuentas administrativas.
     *
     * @return cuentas ordenadas por alta.
     */
    @GetMapping
    public List<AdminAccountResponse> list() {
        return adminAccountService.list();
    }

    /**
     * Crea una cuenta administrativa.
     *
     * @param request datos de la cuenta.
     * @return 201 con la cuenta creada.
     */
    @PostMapping
    public ResponseEntity<AdminAccountResponse> create(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAccountService.create(request));
    }

    /**
     * Cambia el nivel de una cuenta.
     *
     * @param id      identificador de la cuenta.
     * @param request nuevo nivel.
     * @return la cuenta actualizada.
     */
    @PatchMapping("/{id}/level")
    public AdminAccountResponse updateLevel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminLevelRequest request
    ) {
        return adminAccountService.updateLevel(id, request.level());
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * @param id      identificador de la cuenta.
     * @param request estado deseado.
     * @return la cuenta actualizada.
     */
    @PatchMapping("/{id}/status")
    public AdminAccountResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminAccountStatusRequest request
    ) {
        return adminAccountService.setStatus(id, request.active());
    }
}
