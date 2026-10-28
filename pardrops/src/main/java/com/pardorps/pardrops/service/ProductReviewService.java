package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateReviewRequest;
import com.pardorps.pardrops.dto.AdminReviewResponse;
import com.pardorps.pardrops.dto.ProductReviewResponse;
import com.pardorps.pardrops.dto.ReviewEligibilityResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductReview;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio con la lógica de negocio de las reseñas de productos.
 *
 * <p>Restringe la publicación a clientes con sesión iniciada que tengan al
 * menos un pedido <strong>entregado</strong> que contenga el producto, y
 * garantiza una única reseña por cliente y producto. Tras cada alta se
 * recalculan la valoración media y el número de reseñas del producto para que
 * el catálogo refleje siempre las reseñas reales.</p>
 */
@Service
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    /**
     * @param productReviewRepository acceso a las reseñas.
     * @param productRepository       acceso al catálogo (valoración media).
     * @param orderRepository         acceso a los pedidos (validación de compra).
     * @param auditLogService         bitácora de auditoría.
     */
    public ProductReviewService(
            ProductReviewRepository productReviewRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            AuditLogService auditLogService
    ) {
        this.productReviewRepository = productReviewRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Devuelve las reseñas publicadas sobre un producto.
     *
     * @param productId identificador del producto.
     * @return las reseñas, de la más reciente a la más antigua.
     * @throws ApiException si el producto no existe (404).
     */
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getReviews(String productId) {
        requireProduct(productId);
        return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Indica si el usuario actual puede reseñar un producto.
     *
     * <p>Para un visitante anónimo siempre devuelve {@code false} en ambos
     * campos; la comprobación de compra se hace igualmente en el alta.</p>
     *
     * @param productId identificador del producto.
     * @param user      usuario autenticado, o {@code null} si es un invitado.
     * @return el estado de elegibilidad del usuario.
     * @throws ApiException si el producto no existe (404).
     */
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse getEligibility(String productId, User user) {
        requireProduct(productId);

        if (user == null) {
            return new ReviewEligibilityResponse(false, false);
        }

        boolean hasReviewed = productReviewRepository.existsByProductIdAndUserId(
                productId, user.getId());
        boolean hasReceivedProduct = hasReceivedProduct(productId, user);

        return new ReviewEligibilityResponse(!hasReviewed && hasReceivedProduct, hasReviewed);
    }

    /**
     * Publica una reseña de un producto.
     *
     * <p>El autor es siempre el usuario de la sesión; el resto de validaciones
     * (compra entregada y reseña única) se repiten aquí, en el backend, para
     * que no se puedan eludir desde el cliente.</p>
     *
     * @param productId identificador del producto.
     * @param request   calificación y comentario.
     * @param user      usuario autenticado que escribe la reseña.
     * @return la reseña creada.
     * @throws ApiException si no hay sesión (401), el producto no existe (404),
     *                      ya reseñó el producto (409) o no lo ha comprado (403).
     */
    @Transactional
    public ProductReviewResponse createReview(String productId, CreateReviewRequest request, User user) {
        if (user == null) {
            throw ApiException.unauthorized("Debes iniciar sesión para publicar una reseña");
        }

        Product product = requireProduct(productId);

        if (productReviewRepository.existsByProductIdAndUserId(productId, user.getId())) {
            throw ApiException.conflict("Ya has publicado una reseña para este producto");
        }

        if (!hasReceivedProduct(productId, user)) {
            throw ApiException.forbidden(
                    "Solo puedes valorar productos que hayas comprado y recibido");
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment().trim());
        review.setImages(validateImages(request.images()));

        ProductReview saved;
        try {
            saved = productReviewRepository.save(review);
        } catch (DataIntegrityViolationException exception) {
            // Dos peticiones simultáneas: la restricción única de la base de
            // datos impide el duplicado y se responde como un conflicto.
            throw ApiException.conflict("Ya has publicado una reseña para este producto");
        }

        refreshProductRating(product);
        return toResponse(saved);
    }

    /**
     * Comprueba si el usuario tiene al menos un pedido entregado con el producto.
     *
     * @param productId identificador del producto.
     * @param user      usuario a comprobar.
     * @return {@code true} si el producto fue entregado al usuario.
     */
    private boolean hasReceivedProduct(String productId, User user) {
        return orderRepository.existsByUserIdAndItemsProductIdAndStatus(
                user.getId(), productId, OrderStatus.DELIVERED);
    }

    /**
     * Busca el producto o lanza un 404 si no existe.
     *
     * @param productId identificador del producto.
     * @return la entidad del producto.
     * @throws ApiException si el producto no existe.
     */
    private Product requireProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + productId));
    }

    /**
     * Recalcula la valoración media y el número de reseñas del producto.
     *
     * <p>Los valores se guardan en el propio producto para que el catálogo
     * pueda mostrarlos sin tener que calcularlos en cada petición.</p>
     *
     * @param product producto a actualizar.
     */
    private void refreshProductRating(Product product) {
        long reviewCount = productReviewRepository.countByProductId(product.getId());
        Double average = productReviewRepository.averageRatingByProductId(product.getId());

        product.setRating(average == null ? null : Math.round(average * 100.0) / 100.0);
        product.setReviewCount((int) reviewCount);
        productRepository.save(product);
    }

    /**
     * Convierte una entidad de reseña a su representación pública.
     *
     * @param review reseña a convertir.
     * @return la reseña lista para enviar al cliente.
     */
    private ProductReviewResponse toResponse(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getUser().getFullName(),
                review.getImages() == null ? List.of() : List.copyOf(review.getImages()),
                review.getCreatedAt()
        );
    }

    /**
     * Lista todas las reseñas para la moderación del administrador.
     *
     * @return reseñas de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<AdminReviewResponse> findAllForAdmin() {
        return productReviewRepository.findAll(
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /**
     * Elimina una reseña y recalcula la valoración del producto.
     *
     * @param admin    administrador que elimina.
     * @param reviewId identificador de la reseña.
     */
    @Transactional
    public void deleteReview(User admin, Long reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Reseña no encontrada: " + reviewId));
        Product product = review.getProduct();
        String productName = product.getName();
        productReviewRepository.delete(review);
        refreshProductRating(product);
        auditLogService.record(
                admin,
                AuditAction.REVIEW_DELETED,
                "PRODUCT_REVIEW",
                String.valueOf(reviewId),
                "Reseña eliminada del producto " + productName);
    }

    /**
     * Valida las URLs de las imágenes adjuntas.
     *
     * @param images URLs recibidas.
     * @return lista normalizada.
     */
    private List<String> validateImages(List<String> images) {
        if (images == null) {
            return List.of();
        }
        if (images.size() > 3) {
            throw ApiException.badRequest("Puedes adjuntar como máximo 3 imágenes");
        }
        return images.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .peek(url -> {
                    if (!(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/media/"))) {
                        throw ApiException.badRequest("La URL de la imagen no es válida");
                    }
                })
                .toList();
    }

    private AdminReviewResponse toAdminResponse(ProductReview review) {
        Product product = review.getProduct();
        return new AdminReviewResponse(
                review.getId(),
                product.getId(),
                product.getName(),
                review.getRating(),
                review.getComment(),
                review.getUser().getFullName(),
                review.getImages() == null ? List.of() : List.copyOf(review.getImages()),
                review.getCreatedAt()
        );
    }
}
