package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.ProductRequest;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración del catálogo de productos.
 *
 * <p>El administrador ve el catálogo completo (incluidos los productos
 * ocultos y los de los vendedores) y puede moderar su publicación. Solo
 * accesible con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final AuthService authService;

    /**
     * @param productService servicio del catálogo.
     * @param authService    servicio para obtener el usuario autenticado.
     */
    public AdminProductController(ProductService productService, AuthService authService) {
        this.productService = productService;
        this.authService = authService;
    }

    /**
     * Lista todo el catálogo, incluidos los productos ocultos.
     *
     * @return 200 con la lista completa de productos.
     */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.findAll());
    }

    /**
     * Crea un producto de la plataforma (sin vendedor asignado).
     *
     * @param request datos del producto.
     * @return 201 con el producto creado.
     */
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request, null));
    }

    /**
     * Actualiza cualquier producto del catálogo.
     *
     * @param id      identificador del producto.
     * @param request nuevos datos.
     * @return 200 con el producto actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(productService.update(id, request, admin));
    }

    /**
     * Elimina un producto del catálogo.
     *
     * @param id identificador del producto.
     * @return 204 sin contenido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        User admin = authService.getCurrentUser();
        productService.delete(id, admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publica u oculta un producto del catálogo.
     *
     * @param id     identificador del producto.
     * @param active {@code true} para publicarlo, {@code false} para ocultarlo.
     * @return 200 con el producto actualizado.
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Product> changeVisibility(
            @PathVariable String id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(productService.setActive(id, active));
    }
}
