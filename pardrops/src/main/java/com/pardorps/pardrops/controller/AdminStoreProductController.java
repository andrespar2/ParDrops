package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AdminStoreProductSummary;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.service.AdminStoreProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Listado de tiendas con contadores para el catálogo del panel admin.
 *
 * <p>Permite que la sección de productos empiece mostrando tiendas y solo
 * consulte el catálogo de la tienda elegida, en lugar de descargar todos los
 * productos de la plataforma.</p>
 */
@RestController
@RequestMapping("/api/admin/product-stores")
public class AdminStoreProductController {

    private final AdminStoreProductService service;

    public AdminStoreProductController(AdminStoreProductService service) {
        this.service = service;
    }

    /**
     * @param search búsqueda por tienda, slug o vendedor.
     * @param sort   name o products.
     * @param page   página (0-based).
     * @param size   tamaño de página.
     * @return 200 con las tiendas y sus contadores.
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminStoreProductSummary>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(service.summaries(search, sort, page, size));
    }
}
