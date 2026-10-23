package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CategoryRequest;
import com.pardorps.pardrops.dto.CategoryResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.Category;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.CategoryRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del catálogo de categorías en {@link CategoryService}.
 *
 * <p>Cubren el ciclo de vida completo (crear, editar y borrar), la protección
 * contra borrados en uso y la validación de las categorías que se asignan a un
 * producto, que es lo que impide que el catálogo acumule valores huérfanos.</p>
 */
class CategoryServiceTest {

    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final CategoryService categoryService =
            new CategoryService(categoryRepository, productRepository, auditLogService);

    @Test
    @DisplayName("Crea una categoría con slug generado y la registra en la auditoría")
    void createsCategoryWithGeneratedSlug() {
        User admin = admin();
        when(categoryRepository.findByNameIgnoreCase("Ropa Deportiva")).thenReturn(Optional.empty());
        when(categoryRepository.existsBySlug("ropa-deportiva")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse created = categoryService.create(
                admin, new CategoryRequest("Ropa Deportiva", null, "Prendas para entrenar", true, true));

        assertEquals("Ropa Deportiva", created.name());
        assertEquals("ropa-deportiva", created.slug());
        verify(auditLogService).record(eq(admin), eq(AuditAction.CATEGORY_CREATED), eq("CATEGORY"),
                any(), any());
    }

    @Test
    @DisplayName("Rechaza crear una categoría con un nombre ya existente")
    void rejectsDuplicateName() {
        when(categoryRepository.findByNameIgnoreCase("Ropa")).thenReturn(Optional.of(category(1L, "Ropa")));

        ApiException exception = assertThrows(ApiException.class, () -> categoryService.create(
                admin(), new CategoryRequest("Ropa", null, null, true, false)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Actualiza una categoría existente")
    void updatesCategory() {
        Category category = category(1L, "Ropa");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Ropa Casual", 1L)).thenReturn(false);
        when(categoryRepository.existsBySlug("ropa")).thenReturn(true);
        when(categoryRepository.existsBySlug("ropa-casual")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse updated = categoryService.update(
                admin(), 1L, new CategoryRequest("Ropa Casual", null, null, true, false));

        assertEquals("Ropa Casual", updated.name());
        verify(auditLogService).record(any(), eq(AuditAction.CATEGORY_UPDATED), eq("CATEGORY"),
                eq("1"), any());
    }

    @Test
    @DisplayName("Impide borrar una categoría que usan productos")
    void refusesToDeleteCategoryInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Ropa")));
        when(productRepository.countByCategory("Ropa")).thenReturn(4L);

        ApiException exception = assertThrows(ApiException.class,
                () -> categoryService.delete(admin(), 1L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Borra una categoría que no está en uso")
    void deletesUnusedCategory() {
        Category category = category(1L, "Ropa");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.countByCategory("Ropa")).thenReturn(0L);

        categoryService.delete(admin(), 1L);

        verify(categoryRepository).delete(category);
        verify(auditLogService).record(any(), eq(AuditAction.CATEGORY_DELETED), eq("CATEGORY"),
                eq("1"), any());
    }

    @Test
    @DisplayName("Acepta una categoría existente y activa")
    void acceptsExistingActiveCategory() {
        when(categoryRepository.count()).thenReturn(3L);
        when(categoryRepository.findByNameIgnoreCase("Ropa"))
                .thenReturn(Optional.of(category(1L, "Ropa")));

        categoryService.validateAssignable(List.of("Ropa"));
    }

    @Test
    @DisplayName("Rechaza una categoría que no existe cuando hay catálogo")
    void rejectsUnknownCategory() {
        when(categoryRepository.count()).thenReturn(3L);
        when(categoryRepository.findByNameIgnoreCase("Inventada")).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
                () -> categoryService.validateAssignable(List.of("Inventada")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @DisplayName("Rechaza una categoría desactivada")
    void rejectsInactiveCategory() {
        Category category = category(1L, "Ropa");
        category.setActive(false);
        when(categoryRepository.count()).thenReturn(3L);
        when(categoryRepository.findByNameIgnoreCase("Ropa")).thenReturn(Optional.of(category));

        ApiException exception = assertThrows(ApiException.class,
                () -> categoryService.validateAssignable(List.of("Ropa")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    @DisplayName("No valida nada si el catálogo de categorías está vacío")
    void skipsValidationWhenCatalogIsEmpty() {
        when(categoryRepository.count()).thenReturn(0L);

        categoryService.validateAssignable(List.of("Cualquiera"));

        verify(categoryRepository, never()).findByNameIgnoreCase(any());
    }

    @Test
    @DisplayName("Importa las categorías históricas de los productos")
    void importsExistingCategories() {
        when(categoryRepository.findDistinctProductCategories())
                .thenReturn(List.of("Ropa", "Calzado"));
        when(categoryRepository.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        when(categoryRepository.existsBySlug(any())).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int imported = categoryService.importExistingCategories();

        assertEquals(2, imported);
    }

    /**
     * Construye una categoría de prueba.
     *
     * @param id   identificador de la categoría.
     * @param name nombre de la categoría.
     * @return la categoría lista para usar.
     */
    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(name.toLowerCase().replace(" ", "-"));
        category.setActive(true);
        return category;
    }

    /**
     * Construye un administrador de prueba.
     *
     * @return el usuario con rol ADMIN.
     */
    private User admin() {
        User admin = new User();
        admin.setId(9L);
        admin.setEmail("admin@pardrops.com");
        admin.setPassword("hash");
        admin.setFullName("Administrador");
        admin.setRole(Role.ADMIN);
        return admin;
    }
}
