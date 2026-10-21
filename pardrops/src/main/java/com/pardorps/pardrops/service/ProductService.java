package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.ProductRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductImage;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio con la lógica de negocio del catálogo de productos.
 *
 * <p>Cubre tanto la consulta pública (listar y buscar por slug) como la gestión
 * reservada al administrador (crear, editar y eliminar).</p>
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;

    /**
     * @param productRepository      acceso a los productos.
     * @param productReviewRepository acceso a las reseñas (se borran con el producto).
     */
    public ProductService(
            ProductRepository productRepository,
            ProductReviewRepository productReviewRepository
    ) {
        this.productRepository = productRepository;
        this.productReviewRepository = productReviewRepository;
    }

    /**
     * Devuelve todos los productos del catálogo.
     *
     * @return lista de productos.
     */
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Busca un producto por su slug.
     *
     * @param slug slug del producto.
     * @return el producto encontrado.
     * @throws ApiException si no existe ningún producto con ese slug.
     */
    @Transactional(readOnly = true)
    public Product findBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + slug));
    }

    /**
     * Crea un producto nuevo a partir de los datos recibidos.
     *
     * @param request datos del producto.
     * @return el producto creado.
     * @throws ApiException si el slug ya está en uso.
     */
    @Transactional
    public Product create(ProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Ya existe un producto con el slug: " + request.slug());
        }

        Product product = new Product();
        applyRequest(product, request);
        return productRepository.save(product);
    }

    /**
     * Actualiza un producto existente.
     *
     * @param id      identificador del producto.
     * @param request nuevos datos del producto.
     * @return el producto actualizado.
     * @throws ApiException si el producto no existe o el slug choca con otro.
     */
    @Transactional
    public Product update(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));

        if (productRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw ApiException.conflict("Ya existe otro producto con el slug: " + request.slug());
        }

        applyRequest(product, request);
        return productRepository.save(product);
    }

    /**
     * Elimina un producto del catálogo.
     *
     * @param id identificador del producto.
     * @throws ApiException si el producto no existe.
     */
    @Transactional
    public void delete(String id) {
        if (!productRepository.existsById(id)) {
            throw ApiException.notFound("Producto no encontrado: " + id);
        }
        // Las reseñas referencian al producto; se eliminan antes para no
        // violar la clave extranjera de la tabla de reseñas.
        productReviewRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    /**
     * Copia los datos de la petición sobre la entidad del producto.
     *
     * <p>Centraliza el mapeo para que crear y editar compartan exactamente las
     * mismas reglas (valores por defecto, normalización de nulos, etc.).</p>
     *
     * @param product entidad a rellenar.
     * @param request datos de origen.
     */
    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setSlug(request.slug().trim());
        product.setDescription(request.description().trim());
        product.setLongDescription(request.longDescription());
        product.setPrice(request.price());
        product.setOriginalPrice(request.originalPrice());
        product.setCurrency(request.currency() == null || request.currency().isBlank()
                ? "COP" : request.currency());
        product.setStock(request.stock());
        product.setRating(request.rating());
        product.setReviewCount(request.reviewCount());

        // Los booleanos nulos se interpretan como "falso".
        product.setNew(Boolean.TRUE.equals(request.isNew()));
        product.setFeatured(Boolean.TRUE.equals(request.isFeatured()));
        product.setOnSale(Boolean.TRUE.equals(request.onSale()));

        // Las colecciones se reemplazan por copias para que Hibernate detecte
        // correctamente los cambios (no se reutilizan las listas de la petición).
        product.getImages().clear();
        if (request.images() != null) {
            for (ProductImage image : request.images()) {
                product.getImages().add(new ProductImage(image.getUrl(), image.getAlt(), image.isPrimary()));
            }
        }

        product.getCategories().clear();
        if (request.categories() != null) {
            product.getCategories().addAll(new ArrayList<>(request.categories()));
        }

        product.getTags().clear();
        if (request.tags() != null) {
            product.getTags().addAll(new ArrayList<>(request.tags()));
        }

        product.getVariants().clear();
        if (request.variants() != null) {
            for (ProductVariant variant : request.variants()) {
                product.getVariants().add(new ProductVariant(
                        variant.getId(),
                        variant.getName(),
                        variant.getPrice(),
                        variant.getStock(),
                        variant.getSku()
                ));
            }
        }
    }
}
