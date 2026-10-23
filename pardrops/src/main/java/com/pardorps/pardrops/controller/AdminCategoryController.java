package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CategoryRequest;
import com.pardorps.pardrops.dto.CategoryResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración del catálogo de categorías.
 *
 * <p>El administrador crea, edita y desactiva las categorías del marketplace.
 * Una categoría en uso no puede borrarse: se desactiva para que deje de
 * asignarse sin romper los productos que ya la usan. Solo accesible con rol
 * ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    /**
     * @param categoryService servicio de categorías.
     * @param authService     servicio para obtener el usuario autenticado.
     */
    public AdminCategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    /**
     * Lista las categorías del catálogo.
     *
     * @param activeOnly si es {@code true} solo se devuelven las activas.
     * @return 200 con el listado de categorías.
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(categoryService.findAll(activeOnly));
    }

    /**
     * Devuelve una categoría por su identificador.
     *
     * @param id identificador de la categoría.
     * @return 200 con la categoría.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    /**
     * Crea una categoría nueva.
     *
     * @param request datos de la categoría.
     * @return 201 con la categoría creada.
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(admin, request));
    }

    /**
     * Actualiza una categoría existente.
     *
     * @param id      identificador de la categoría.
     * @param request nuevos datos.
     * @return 200 con la categoría actualizada.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        User admin = authService.getCurrentUser();
        return ResponseEntity.ok(categoryService.update(admin, id, request));
    }

    /**
     * Elimina una categoría que no esté en uso.
     *
     * @param id identificador de la categoría.
     * @return 204 sin contenido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User admin = authService.getCurrentUser();
        categoryService.delete(admin, id);
        return ResponseEntity.noContent().build();
    }
}
