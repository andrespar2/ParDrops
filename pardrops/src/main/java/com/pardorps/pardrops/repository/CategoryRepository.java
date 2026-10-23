package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso al catálogo de categorías.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Busca una categoría por su slug.
     *
     * @param slug slug a buscar.
     * @return la categoría si existe.
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Busca una categoría por su nombre (sin distinguir mayúsculas).
     *
     * @param name nombre a buscar.
     * @return la categoría si existe.
     */
    @Query("select c from Category c where lower(c.name) = lower(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Comprueba si ya existe una categoría con ese slug.
     *
     * @param slug slug a comprobar.
     * @return {@code true} si ya existe.
     */
    boolean existsBySlug(String slug);

    /**
     * Comprueba si otra categoría (con distinto id) usa ya ese slug.
     *
     * @param slug slug a comprobar.
     * @param id   identificador de la categoría que se está editando.
     * @return {@code true} si otra categoría usa el slug.
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Comprueba si otra categoría (con distinto id) usa ya ese nombre.
     *
     * @param name nombre a comprobar.
     * @param id   identificador de la categoría que se está editando.
     * @return {@code true} si otra categoría usa el nombre.
     */
    @Query("select count(c) > 0 from Category c "
            + "where lower(c.name) = lower(:name) and c.id <> :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    /**
     * Devuelve el catálogo de categorías ordenado por nombre.
     *
     * @return listado de categorías.
     */
    List<Category> findAllByOrderByNameAsc();

    /**
     * Devuelve las categorías activas ordenadas por nombre.
     *
     * @return listado de categorías disponibles.
     */
    List<Category> findByActiveTrueOrderByNameAsc();

    /**
     * Cuenta cuántos productos usan la categoría indicada.
     *
     * <p>Se consulta la tabla de valores del producto ({@code product_categories})
     * porque los productos guardan el nombre de la categoría y no una clave
     * ajena. Así el borrado de categorías en uso se puede impedir con datos
     * reales.</p>
     *
     * @param name nombre de la categoría.
     * @return número de productos que la usan.
     */
    @Query(value = "select count(*) from product_categories where lower(category) = lower(:name)",
            nativeQuery = true)
    long countProductsUsingCategory(@Param("name") String name);

    /**
     * Devuelve los nombres de categoría distintos que usan los productos.
     *
     * <p>Se usa en la migración inicial para importar las categorías que ya
     * existían como texto libre.</p>
     *
     * @return nombres de categoría distintos.
     */
    @Query(value = "select distinct category from product_categories order by category",
            nativeQuery = true)
    List<String> findDistinctProductCategories();
}
