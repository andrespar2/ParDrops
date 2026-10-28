package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.ProductReviewResponse;
import com.pardorps.pardrops.dto.StoreRequest;
import com.pardorps.pardrops.dto.StoreResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductReview;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Servicio con la lógica de las tiendas de los vendedores.
 *
 * <p>Gestiona tanto la tienda pública (visible para cualquier visitante) como
 * la edición del perfil comercial por parte del propio vendedor.</p>
 */
@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    /**
     * @param storeRepository   acceso a las tiendas.
     * @param productRepository acceso al catálogo para contar los productos.
     * @param reviewRepository  acceso a las reseñas para el rating de la tienda.
     */
    public StoreService(
            StoreRepository storeRepository,
            ProductRepository productRepository,
            ProductReviewRepository reviewRepository
    ) {
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Devuelve la tienda pública de un vendedor por su slug.
     *
     * @param slug slug público de la tienda.
     * @return la tienda con su recuento de productos publicados.
     * @throws ApiException si no existe ninguna tienda con ese slug (404).
     */
    @Transactional(readOnly = true)
    public StoreResponse findPublicBySlug(String slug) {
        Store store = storeRepository.findBySlugWithOwner(slug)
                .orElseThrow(() -> ApiException.notFound("Tienda no encontrada: " + slug));
        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw ApiException.notFound("Tienda no encontrada: " + slug);
        }
        return toResponse(store, countPublishedProducts(store));
    }

    /**
     * Devuelve la tienda del vendedor autenticado.
     *
     * @param seller vendedor autenticado.
     * @return la tienda del vendedor.
     * @throws ApiException si el usuario todavía no tiene tienda (404).
     */
    @Transactional(readOnly = true)
    public StoreResponse findOwnStore(User seller) {
        Store store = storeRepository.findBySellerIdWithOwner(seller.getId())
                .orElseThrow(() -> ApiException.notFound(
                        "Todavía no tienes una tienda en ParDrops"));
        return toResponse(store, countPublishedProducts(store));
    }

    /**
     * Devuelve todas las tiendas activas, ordenadas por nombre.
     *
     * @return listado de tiendas.
     */
    @Transactional(readOnly = true)
    public List<StoreResponse> findAllActive() {
        return storeRepository.findAllByOrderByNameAsc()
                .stream()
                .filter(store -> store.getStatus() == StoreStatus.ACTIVE)
                .map(store -> toResponse(store, countPublishedProducts(store)))
                .toList();
    }

    /**
     * Actualiza el perfil comercial de la tienda del vendedor autenticado.
     *
     * @param seller  vendedor autenticado.
     * @param request datos a actualizar.
     * @return la tienda actualizada.
     * @throws ApiException si el usuario todavía no tiene tienda (404).
     */
    @Transactional
    public StoreResponse updateOwnStore(User seller, StoreRequest request) {
        Store store = storeRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> ApiException.notFound(
                        "Todavía no tienes una tienda en ParDrops"));

        store.setDescription(trimToNull(request.description()));
        store.setLogoUrl(trimToNull(request.logoUrl()));
        store.setBannerUrl(trimToNull(request.bannerUrl()));
        store.setPublicEmail(trimToNull(request.publicEmail()));
        store.setPublicPhone(trimToNull(request.publicPhone()));
        store.setCity(trimToNull(request.city()));
        store.setWhatsapp(trimToNull(request.whatsapp()));

        String name = trimToNull(request.name());
        if (name != null) {
            store.setName(name);
        }

        String slug = trimToNull(request.slug());
        if (slug != null && !slug.equals(store.getSlug())) {
            if (storeRepository.existsBySlugAndIdNot(slug, store.getId())) {
                throw ApiException.conflict(
                        "Ya existe otra tienda con el slug: " + slug);
            }
            store.setSlug(slug);
        }

        Store saved = storeRepository.save(store);
        return toResponse(saved, countPublishedProducts(saved));
    }

    /**
     * Suspende o reactiva la tienda de un vendedor.
     *
     * <p>Los productos de una tienda suspendida dejan de publicarse, pero no se
     * borran: al reactivarla vuelven a estar disponibles.</p>
     *
     * @param sellerId identificador del vendedor.
     * @param status  nuevo estado de la tienda.
     * @return la tienda con el nuevo estado.
     * @throws ApiException si el vendedor no tiene tienda (404).
     */
    @Transactional
    public StoreResponse changeStatus(Long sellerId, StoreStatus status) {
        Store store = storeRepository.findBySellerId(sellerId)
                .orElseThrow(() -> ApiException.notFound(
                        "El vendedor no tiene tienda registrada"));
        store.setStatus(status);
        Store saved = storeRepository.save(store);
        return toResponse(saved, countPublishedProducts(saved));
    }

    /**
     * Genera un slug único a partir del nombre comercial.
     *
     * @param businessName nombre comercial del negocio.
     * @return slug en minúsculas y sin espacios.
     */
    public String buildSlug(String businessName) {
        String base = businessName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) {
            base = "tienda";
        }
        String candidate = base;
        int suffix = 1;
        while (storeRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    /**
     * Cuenta los productos publicados de una tienda.
     *
     * @param store tienda cuya se quiere contar el catálogo.
     * @return número de productos publicados.
     */
    private long countPublishedProducts(Store store) {
        if (store.getSeller() == null) {
            return 0L;
        }
        return productRepository.countPublishedBySellerId(store.getSeller().getId());
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
     * Convierte una entidad de tienda en su representación pública.
     *
     * @param store         tienda a convertir.
     * @param productCount  número de productos publicados.
     * @return la tienda lista para enviar al cliente.
     */
    private StoreResponse toResponse(Store store, long productCount) {
        double rating = 0.0;
        long reviewCount = 0;
        if (store.getSeller() != null) {
            Double average = reviewRepository.averageRatingBySellerId(store.getSeller().getId());
            rating = average == null ? 0.0 : Math.round(average * 100.0) / 100.0;
            reviewCount = reviewRepository.countByProductSellerId(store.getSeller().getId());
        }
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getPublicEmail(),
                store.getPublicPhone(),
                store.getCity(),
                store.getWhatsapp(),
                store.getStatus(),
                rating,
                reviewCount,
                productCount,
                store.getCreatedAt()
        );
    }

    /**
     * Devuelve las reseñas recientes de los productos de una tienda.
     *
     * @param slug slug público de la tienda.
     * @return reseñas con autor, estrellas y comentario.
     */
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> findReviews(String slug) {
        Store store = storeRepository.findBySlugWithOwner(slug)
                .orElseThrow(() -> ApiException.notFound("Tienda no encontrada: " + slug));
        if (store.getStatus() != StoreStatus.ACTIVE || store.getSeller() == null) {
            throw ApiException.notFound("Tienda no encontrada: " + slug);
        }
        return reviewRepository.findRecentBySellerId(
                        store.getSeller().getId(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .stream()
                .map(this::toReviewResponse)
                .toList();
    }

    private ProductReviewResponse toReviewResponse(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getUser() != null ? review.getUser().getFullName() : "Cliente",
                review.getImages() == null ? List.of() : List.copyOf(review.getImages()),
                review.getCreatedAt()
        );
    }
}
