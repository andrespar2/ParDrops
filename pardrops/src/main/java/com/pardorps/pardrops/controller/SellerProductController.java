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
 * API de gestión del catálogo de un vendedor.
 *
 * <p>El vendedor solo ve y manages sus propios productos: la comprobación de
 * propiedad la hace {@link ProductService}, no la interfaz. Solo accesible con
 * rol SELLER.</p>
 */
@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final ProductService productService;
    private final AuthService authService;

    /**
     * @param productService servicio del catálogo.
     * @param authService    servicio para obtener el usuario autenticado.
     */
    public SellerProductController(ProductService productService, AuthService authService) {
        this.productService = productService;
        this.authService = authService;
    }

    /**
     * Lista los productos del vendedor autenticado.
     *
     * @return 200 con la lista de productos del vendedor.
     */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(productService.findBySeller(seller));
    }

    /**
     * Publica un producto nuevo en la tienda del vendedor.
     *
     * @param request datos del producto.
     * @return 201 con el producto creado.
     */
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        User seller = authService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(request, seller));
    }

    /**
     * Actualiza uno de los productos del vendedor.
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
        User seller = authService.getCurrentUser();
        return ResponseEntity.ok(productService.update(id, request, seller));
    }

    /**
     * Elimina uno de los productos del vendedor.
     *
     * @param id identificador del producto.
     * @return 204 sin contenido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        User seller = authService.getCurrentUser();
        productService.delete(id, seller);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publica u oculta uno de los productos del vendedor.
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
        User seller = authService.getCurrentUser();
        Product product = productService.findOwned(id, seller);
        return ResponseEntity.ok(productService.setActive(product.getId(), active));
    }
}
