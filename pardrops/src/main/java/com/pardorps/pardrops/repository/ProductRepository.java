package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso al catálogo de productos.
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Busca un producto por su slug (identificador legible en la URL).
     *
     * @param slug slug a buscar.
     * @return el producto si existe.
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Comprueba si ya existe un producto con ese slug.
     *
     * @param slug slug a comprobar.
     * @return {@code true} si ya existe.
     */
    boolean existsBySlug(String slug);

    /**
     * Comprueba si otro producto (con distinto id) usa ya ese slug.
     * Se usa al editar para permitir conservar el propio slug.
     *
     * @param slug slug a comprobar.
     * @param id   identificador del producto que se está editando.
     * @return {@code true} si otro producto usa el slug.
     */
    boolean existsBySlugAndIdNot(String slug, String id);
}
