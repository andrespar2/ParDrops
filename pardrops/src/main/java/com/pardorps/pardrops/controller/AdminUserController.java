package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminSellerResponse;
import com.pardorps.pardrops.dto.AdminUserDetailResponse;
import com.pardorps.pardrops.dto.AdminUserResponse;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.dto.UserStatusRequest;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
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
 * <p>Permite consultar y filtrar las cuentas de la plataforma, ver la ficha de
 * un vendedor con su actividad y cambiar el estado de una cuenta (activa o
 * suspendida). Solo accesible con rol ADMIN.</p>
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
     * Lista las cuentas de la plataforma, con búsqueda, filtros y paginación.
     *
     * <p>El listado completo (sin paginar) se mantiene disponible para no romper
     * a los clientes anteriores: si no se indica {@code page} ni {@code size} se
     * devuelve la lista entera.</p>
     *
     * @param search texto de búsqueda por correo, nombre o teléfono (opcional).
     * @param role   filtro por rol (opcional).
     * @param status filtro por estado (opcional).
     * @param page   número de página (empezando en 0; opcional).
     * @param size   tamaño de página (opcional).
     * @return 200 con el listado de cuentas o con la página solicitada.
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page == null && size == null && role == null && status == null) {
            return ResponseEntity.ok(adminUserService.search(search));
        }

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 20 : size;
        PageResponse<AdminUserResponse> result =
                adminUserService.searchPage(search, role, status, pageNumber, pageSize);
        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve la ficha completa de una cuenta.
     *
     * @param id identificador de la cuenta.
     * @return 200 con la cuenta, su tienda y su actividad.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.findDetail(id));
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

/**
 * API del listado de vendedores del marketplace.
 *
 * <p>Se expone en {@code /api/admin/sellers} para que el panel de
 * administración pueda consultar las tiendas, su estado y su actividad sin
 * recorrer el listado completo de cuentas.</p>
 */
@RestController
@RequestMapping("/api/admin/sellers")
class AdminSellerController {

    private final AdminUserService adminUserService;

    /**
     * @param adminUserService servicio de administración (incluye vendedores).
     */
    AdminSellerController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Lista los vendedores con su tienda y sus cifras de actividad.
     *
     * @param status filtro por estado de la tienda (opcional).
     * @param search texto de búsqueda por vendedor, correo o tienda (opcional).
     * @return 200 con el listado de vendedores.
     */
    @GetMapping
    public ResponseEntity<List<AdminSellerResponse>> list(
            @RequestParam(required = false) StoreStatus status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(adminUserService.findSellers(status, search));
    }
}
