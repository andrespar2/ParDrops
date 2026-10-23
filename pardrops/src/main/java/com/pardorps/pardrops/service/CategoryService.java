package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CategoryRequest;
import com.pardorps.pardrops.dto.CategoryResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.Category;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.CategoryRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Servicio con la lógica del catálogo de categorías.
 *
 * <p>Las categorías pasan de ser texto libre dentro del producto a un catálogo
 * gestionable. El nombre sigue siendo el vínculo con los productos (guardan el
 * nombre, no una clave ajena), de modo que el catálogo histórico sigue
 * funcionando aunque el administrador reorganice las categorías.</p>
 */
@Service
public class CategoryService {

    /** Tipo de entidad que se registra en la bitácora de auditoría. */
    private static final String ENTITY_TYPE = "CATEGORY";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    /**
     * @param categoryRepository acceso a las categorías.
     * @param productRepository  acceso al catálogo (para contar usos).
     * @param auditLogService    bitácora de auditoría.
     */
    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            AuditLogService auditLogService
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Devuelve el catálogo de categorías.
     *
     * @param activeOnly si es {@code true} solo se devuelven las activas.
     * @return listado de categorías ordenado por nombre.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(boolean activeOnly) {
        List<Category> categories = activeOnly
                ? categoryRepository.findByActiveTrueOrderByNameAsc()
                : categoryRepository.findAllByOrderByNameAsc();
        return categories.stream().map(this::toResponse).toList();
    }

    /**
     * Devuelve una categoría por su identificador.
     *
     * @param id identificador de la categoría.
     * @return la categoría encontrada.
     * @throws ApiException si no existe (404).
     */
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(require(id));
    }

    /**
     * Devuelve los nombres de categoría que un producto puede usar.
     *
     * @return nombres ordenados alfabéticamente.
     */
    @Transactional(readOnly = true)
    public List<String> findAvailableNames() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(Category::getName)
                .toList();
    }

    /**
     * Crea una categoría nueva.
     *
     * @param admin   administrador que realiza la operación.
     * @param request datos de la categoría.
     * @return la categoría creada.
     * @throws ApiException si el nombre o el slug ya están en uso (409).
     */
    @Transactional
    public CategoryResponse create(User admin, CategoryRequest request) {
        String name = request.name().trim();

        categoryRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw ApiException.conflict("Ya existe una categoría con el nombre: " + name);
        });

        Category category = new Category();
        category.setName(name);
        category.setSlug(resolveSlug(request.slug(), name));
        category.setDescription(trimToNull(request.description()));
        category.setActive(request.active() == null || request.active());
        category.setFeatured(Boolean.TRUE.equals(request.featured()));

        Category saved = categoryRepository.save(category);
        auditLogService.record(admin, AuditAction.CATEGORY_CREATED, ENTITY_TYPE,
                String.valueOf(saved.getId()), "Categoría creada: " + saved.getName());
        return toResponse(saved);
    }

    /**
     * Actualiza una categoría existente.
     *
     * @param admin   administrador que realiza la operación.
     * @param id      identificador de la categoría.
     * @param request nuevos datos.
     * @return la categoría actualizada.
     * @throws ApiException si la categoría no existe (404) o si el nombre o el
     *                     slug chocan con otra categoría (409).
     */
    @Transactional
    public CategoryResponse update(User admin, Long id, CategoryRequest request) {
        Category category = require(id);
        String name = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("Ya existe otra categoría con el nombre: " + name);
        }

        category.setName(name);
        category.setSlug(resolveSlug(request.slug(), name));
        category.setDescription(trimToNull(request.description()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.featured() != null) {
            category.setFeatured(request.featured());
        }

        Category saved = categoryRepository.save(category);
        auditLogService.record(admin, AuditAction.CATEGORY_UPDATED, ENTITY_TYPE,
                String.valueOf(saved.getId()), "Categoría actualizada: " + saved.getName());
        return toResponse(saved);
    }

    /**
     * Elimina una categoría que no esté en uso.
     *
     * @param admin administrador que realiza la operación.
     * @param id    identificador de la categoría.
     * @throws ApiException si la categoría no existe (404) o si hay productos
     *                     que la usan (409).
     */
    @Transactional
    public void delete(User admin, Long id) {
        Category category = require(id);

        long inUse = productRepository.countByCategory(category.getName());
        if (inUse > 0) {
            throw ApiException.conflict("No se puede borrar la categoría \""
                    + category.getName() + "\": la usan " + inUse + " productos.");
        }

        categoryRepository.delete(category);
        auditLogService.record(admin, AuditAction.CATEGORY_DELETED, ENTITY_TYPE,
                String.valueOf(id), "Categoría eliminada: " + category.getName());
    }

    /**
     * Importa como categorías los nombres que ya usaban los productos.
     *
     * <p>Se ejecuta una sola vez, durante la migración de esquema, para que los
     * valores históricos sigan siendo válidos al activar la validación de
     * categorías.</p>
     *
     * @return número de categorías importadas.
     */
    @Transactional
    public int importExistingCategories() {
        List<String> names = categoryRepository.findDistinctProductCategories();
        int imported = 0;

        for (String rawName : names) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim();
            if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
                continue;
            }
            Category category = new Category();
            category.setName(name);
            category.setSlug(resolveSlug(null, name));
            category.setActive(true);
            categoryRepository.save(category);
            imported++;
        }

        return imported;
    }

    /**
     * Comprueba que todas las categorías indicadas existen y están activas.
     *
     * <p>La validación solo se aplica cuando el catálogo de categorías tiene
     * contenido: así una instalación recién creada (o una prueba unitaria que no
     * siembra categorías) no queda bloqueada, y los valores históricos
     * importados por la migración siempre pasan.</p>
     *
     * @param names nombres de categoría que se quieren asignar al producto.
     * @throws ApiException si alguna categoría no existe o está desactivada (400).
     */
    @Transactional(readOnly = true)
    public void validateAssignable(List<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        if (categoryRepository.count() == 0) {
            return;
        }

        for (String rawName : names) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim();
            Category category = categoryRepository.findByNameIgnoreCase(name)
                    .orElseThrow(() -> ApiException.badRequest(
                            "La categoría \"" + name + "\" no existe. Créala antes de asignarla."));
            if (!category.isActive()) {
                throw ApiException.badRequest(
                        "La categoría \"" + name + "\" está desactivada.");
            }
        }
    }

    /**
     * Busca una categoría o lanza un error si no existe.
     *
     * @param id identificador de la categoría.
     * @return la categoría encontrada.
     * @throws ApiException si no existe (404).
     */
    private Category require(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Categoría no encontrada: " + id));
    }

    /**
     * Genera un slug único para la categoría.
     *
     * @param requested slug solicitado (puede ser nulo).
     * @param name      nombre de la categoría.
     * @return un slug único en minúsculas.
     */
    private String resolveSlug(String requested, String name) {
        String base = requested == null || requested.isBlank() ? name : requested;
        String slug = base.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "categoria";
        }

        String candidate = slug;
        int suffix = 1;
        while (categoryRepository.existsBySlug(candidate)) {
            candidate = slug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    /**
     * Normaliza un texto opcional: recorta los espacios y convierte la cadena
     * vacía en {@code null}.
     *
     * @param value texto de origen.
     * @return el texto normalizado o {@code null}.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Convierte una categoría en su representación para el panel.
     *
     * @param category categoría a convertir.
     * @return la categoría lista para enviar al cliente.
     */
    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.isFeatured(),
                productRepository.countByCategory(category.getName()),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
