package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.BulkActionResponse;
import com.pardorps.pardrops.dto.BulkIdsRequest;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AdminStoreService;
import com.pardorps.pardrops.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Acciones administrativas sobre tiendas.
 *
 * <p>Todas las rutas están bajo {@code /api/admin/**} y, por tanto, exigen rol
 * ADMIN. La eliminación es lógica y conserva el historial.</p>
 */
@RestController
@RequestMapping("/api/admin/stores")
public class AdminStoreController {

    private final AdminStoreService adminStoreService;
    private final AuthService authService;

    public AdminStoreController(AdminStoreService adminStoreService, AuthService authService) {
        this.adminStoreService = adminStoreService;
        this.authService = authService;
    }

    /**
     * Elimina una tienda.
     *
     * @param storeId identificador de la tienda.
     * @return resultado de la eliminación.
     */
    @DeleteMapping("/{storeId}")
    public ResponseEntity<BulkActionResponse> delete(@PathVariable Long storeId) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(adminStoreService.deleteStores(List.of(storeId), admin));
    }

    /**
     * Elimina varias tiendas seleccionadas.
     *
     * @param request tiendas seleccionadas.
     * @return resultado de la acción en masa.
     */
    @PostMapping("/bulk-delete")
    public ResponseEntity<BulkActionResponse> bulkDelete(
            @Valid @RequestBody BulkIdsRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(adminStoreService.deleteStores(request.ids(), admin));
    }

    /**
     * Restaura una tienda eliminada.
     *
     * @param storeId identificador de la tienda.
     * @return resultado de la restauración.
     */
    @PatchMapping("/{storeId}/restore")
    public ResponseEntity<BulkActionResponse> restore(@PathVariable Long storeId) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(adminStoreService.restoreStore(storeId, admin));
    }
}
