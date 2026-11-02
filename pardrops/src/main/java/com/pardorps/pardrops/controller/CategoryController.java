package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CategoryResponse;
import com.pardorps.pardrops.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo público de categorías.
 *
 * <p>Cualquier visitante puede consultar las categorías activas para navegar el
 * marketplace; la administración las gestiona por su propia API.</p>
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * @param categoryService servicio de categorías.
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Lista las categorías activas.
     *
     * @return categorías activas ordenadas.
     */
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll(true);
    }
}
