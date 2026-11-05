package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Devuelve los productos publicados, con su vendedor cargado.
     *
     * <p>Es la consulta que usa el catálogo público: solo aparecen los
     * productos activos, tanto los de la tienda propia como los de los
     * vendedores. Los productos de un vendedor cuya tienda está suspendida no
     * se muestran, porque el catálogo público ignora las tiendas suspendidas.</p>
     *
     * @return listado de productos publicados.
     */
    @Query("select distinct p from Product p left join fetch p.seller "
            + "where p.active = true and (p.seller is null or exists "
            + "(select s.id from Store s where s.seller.id = p.seller.id "
            + "and s.status = com.pardorps.pardrops.model.StoreStatus.ACTIVE))")
    List<Product> findPublished();

    /**
     * Cuenta los productos publicados sin cargarlos en memoria.
     *
     * @return total de productos visibles en el catálogo público.
     */
    @Query("select count(distinct p) from Product p "
            + "where p.active = true and (p.seller is null or exists "
            + "(select s.id from Store s where s.seller.id = p.seller.id "
            + "and s.status = com.pardorps.pardrops.model.StoreStatus.ACTIVE))")
    long countPublished();

    /**
     * Devuelve todos los productos con su vendedor cargado (uso del panel de
     * administración).
     *
     * @return listado completo de productos.
     */
    @Query("select distinct p from Product p left join fetch p.seller")
    List<Product> findAllWithSeller();

    /**
     * Devuelve los productos de un vendedor concreto con su vendedor cargado.
     *
     * @param sellerId identificador del vendedor.
     * @return listado de productos del vendedor.
     */
    @Query("select distinct p from Product p left join fetch p.seller where p.seller.id = :sellerId")
    List<Product> findBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Devuelve los productos publicados de un vendedor concreto.
     *
     * @param sellerId identificador del vendedor.
     * @return listado de productos publicados del vendedor.
     */
    @Query("select distinct p from Product p left join fetch p.seller "
            + "where p.seller.id = :sellerId and p.active = true")
    List<Product> findPublishedBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Cuenta los productos publicados de un vendedor.
     *
     * <p>Se escribe como consulta JPQL explícita y no como nombre derivado:
     * el producto tiene un atributo transitorio llamado {@code sellerId} (el
     * que consume el frontend) y Spring Data lo interpretaría como una
     * propiedad persistente que no existe en la base de datos.</p>
     *
     * @param sellerId identificador del vendedor.
     * @return número de productos publicados.
     */
    @Query("select count(p) from Product p "
            + "where p.seller.id = :sellerId and p.active = true")
    long countPublishedBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Cuenta todos los productos de un vendedor, publicados u ocultos.
     *
     * @param sellerId identificador del vendedor.
     * @return número de productos del vendedor.
     */
    @Query("select count(p) from Product p where p.seller.id = :sellerId")
    long countBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Cuenta los productos sin existencias.
     *
     * @return número de productos con stock cero.
     */
    @Query("select count(p) from Product p where p.stock = 0")
    long countOutOfStock();

    /**
     * Cuenta los productos publicados sin existencias de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return número de productos publicados con stock cero.
     */
    @Query("select count(p) from Product p "
            + "where p.seller.id = :sellerId and p.active = true and p.stock = 0")
    long countOutOfStockBySellerId(@Param("sellerId") Long sellerId);

    /**
     * Busca productos aplicando filtros de panel de administración.
     *
     * <p>Todos los filtros son opcionales: si llegan nulos o vacíos no se
     * aplican. La búsqueda por texto recorre nombre, slug y descripción.</p>
     *
     * @param search    texto de búsqueda (puede ser nulo).
     * @param sellerId  filtra por vendedor propietario (puede ser nulo).
     * @param active    filtra por visibilidad (puede ser nulo).
     * @param pageable  paginación y orden.
     * @return página de productos que cumplen los filtros.
     */
    @Query("select distinct p from Product p left join fetch p.seller "
            + "where (lower(p.name) like lower(concat('%', :search, '%')) "
            + "   or lower(p.slug) like lower(concat('%', :search, '%')) "
            + "   or lower(p.description) like lower(concat('%', :search, '%'))) "
            + "  and (:sellerId is null or (:sellerId = -1 and p.seller is null) "
            + "       or p.seller.id = :sellerId) "
            + "  and (:active is null or p.active = :active)")
    Page<Product> searchForAdmin(
            @Param("search") String search,
            @Param("sellerId") Long sellerId,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Busca productos por categoría en el panel de administración.
     *
     * <p>Se mantiene separada de la búsqueda general para no enviar nunca un
     * parámetro nulo a {@code lower()}, que PostgreSQL no puede tipar.</p>
     *
     * @param search    texto de búsqueda (puede ser nulo).
     * @param sellerId  filtra por vendedor propietario (puede ser nulo).
     * @param category  categoría obligatoria en esta consulta.
     * @param active    filtra por visibilidad (puede ser nulo).
     * @param pageable  paginación y orden.
     * @return página de productos que cumplen los filtros.
     */
    @Query("select distinct p from Product p left join fetch p.seller "
            + "where (lower(p.name) like lower(concat('%', :search, '%')) "
            + "   or lower(p.slug) like lower(concat('%', :search, '%')) "
            + "   or lower(p.description) like lower(concat('%', :search, '%'))) "
            + "  and (:sellerId is null or (:sellerId = -1 and p.seller is null) "
            + "       or p.seller.id = :sellerId) "
            + "  and (:active is null or p.active = :active) "
            + "  and exists ("
            + "        select 1 from Product p2 join p2.categories c "
            + "        where p2.id = p.id and lower(c) = lower(:category))")
    Page<Product> searchForAdminByCategory(
            @Param("search") String search,
            @Param("sellerId") Long sellerId,
            @Param("category") String category,
            @Param("active") Boolean active,
            Pageable pageable);

    /**
     * Cuenta los productos publicados que pertenecen a una categoría.
     *
     * @param name nombre de la categoría.
     * @return número de productos publicados.
     */
    @Query("select count(distinct p) from Product p join p.categories c "
            + "where p.active = true and lower(c) = lower(:name)")
    long countPublishedByCategory(@Param("name") String name);

    /**
     * Cuenta todos los productos que pertenecen a una categoría.
     *
     * @param name nombre de la categoría.
     * @return número de productos.
     */
    @Query("select count(distinct p) from Product p join p.categories c "
            + "where lower(c) = lower(:name)")
    long countByCategory(@Param("name") String name);

    /**
     * Productos mejor valorados con al menos una reseña.
     *
     * @param pageable límite de resultados.
     * @return productos ordenados por media y número de reseñas.
     */
    @Query("""
            select p from Product p
            where p.active = true and p.reviewCount > 0
            order by p.rating desc, p.reviewCount desc
            """)
    List<Product> findTopRated(Pageable pageable);

    /**
     * Busca productos por una colección de identificadores.
     *
     * @param ids identificadores.
     * @return productos encontrados.
     */
    List<Product> findByIdIn(java.util.Collection<String> ids);

    /**
     * Devuelve solo los campos escalares necesarios para construir el ranking
     * del catálogo. Evita cargar imágenes, variantes y categorías de todo el
     * catálogo en cada petición.
     *
     * @param search  texto de búsqueda (cadena vacía para no filtrar).
     * @param category categoría exacta (nula para no filtrar).
     * @param minPrice precio mínimo (nulo para no filtrar).
     * @param maxPrice precio máximo (nulo para no filtrar).
     * @param onlyNew solo novedades cuando es verdadero.
     * @param onlyOnSale solo ofertas cuando es verdadero.
     * @return filas con id, rating, reviewCount, createdAt, featured, isNew,
     *         stock, price y sellerId.
     */
    @Query("""
            select p.id, p.rating, p.reviewCount, p.createdAt, p.isFeatured,
                   p.isNew, p.stock, p.price, p.seller.id
            from Product p
            where p.active = true
              and (p.seller is null or exists (
                    select s.id from Store s
                    where s.seller.id = p.seller.id
                      and s.status = com.pardorps.pardrops.model.StoreStatus.ACTIVE))
              and (:search = '' or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.slug) like lower(concat('%', :search, '%'))
                   or lower(p.description) like lower(concat('%', :search, '%')))
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:onlyNew = false or p.isNew = true)
              and (:onlyOnSale = false or p.onSale = true)
            """)
    List<Object[]> findRankingCandidates(
            @Param("search") String search,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("onlyNew") boolean onlyNew,
            @Param("onlyOnSale") boolean onlyOnSale);

    /**
     * Variante con categoría obligatoria. Se mantiene separada para no enviar
     * nunca un parámetro nulo a {@code lower()}, que PostgreSQL no puede tipar.
     *
     * @param search  texto de búsqueda (cadena vacía para no filtrar).
     * @param category categoría exacta obligatoria.
     * @param minPrice precio mínimo (nulo para no filtrar).
     * @param maxPrice precio máximo (nulo para no filtrar).
     * @param onlyNew solo novedades cuando es verdadero.
     * @param onlyOnSale solo ofertas cuando es verdadero.
     * @return filas con id, rating, reviewCount, createdAt, featured, isNew,
     *         stock, price y sellerId.
     */
    @Query("""
            select p.id, p.rating, p.reviewCount, p.createdAt, p.isFeatured,
                   p.isNew, p.stock, p.price, p.seller.id
            from Product p
            where p.active = true
              and (p.seller is null or exists (
                    select s.id from Store s
                    where s.seller.id = p.seller.id
                      and s.status = com.pardorps.pardrops.model.StoreStatus.ACTIVE))
              and (:search = '' or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.slug) like lower(concat('%', :search, '%'))
                   or lower(p.description) like lower(concat('%', :search, '%')))
              and exists (
                    select 1 from Product p2 join p2.categories c
                    where p2.id = p.id and lower(c) = lower(:category))
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:onlyNew = false or p.isNew = true)
              and (:onlyOnSale = false or p.onSale = true)
            """)
    List<Object[]> findRankingCandidatesByCategory(
            @Param("search") String search,
            @Param("category") String category,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("onlyNew") boolean onlyNew,
            @Param("onlyOnSale") boolean onlyOnSale);

    /**
     * Devuelve la pareja producto/categoría de los productos publicados.
     *
     * <p>Se usa para calcular afinidad con las categorías compradas por el
     * usuario sin cargar las entidades completas.</p>
     *
     * @param ids identificadores a consultar.
     * @return filas con id de producto y nombre de categoría.
     */
    @Query("""
            select p.id, c from Product p join p.categories c
            where p.id in :ids
            """)
    List<Object[]> findCategoryPairsByProductIds(@Param("ids") java.util.Collection<String> ids);

    /**
     * Cuenta los productos por vendedor en una sola consulta agregada.
     *
     * @return filas con sellerId y total de productos.
     */
    @Query("""
            select p.seller.id, count(p) from Product p
            where p.seller is not null
            group by p.seller.id
            """)
    List<Object[]> countGroupedBySeller();

    /**
     * Cuenta los productos publicados por vendedor en una sola consulta.
     *
     * @return filas con sellerId y total publicado.
     */
    @Query("""
            select p.seller.id, count(p) from Product p
            where p.seller is not null and p.active = true
            group by p.seller.id
            """)
    List<Object[]> countPublishedGroupedBySeller();

    /**
     * Cuenta los productos publicados del catálogo propio (sin vendedor).
     *
     * @return total de productos propios publicados.
     */
    long countBySellerIsNullAndActiveTrue();
}
