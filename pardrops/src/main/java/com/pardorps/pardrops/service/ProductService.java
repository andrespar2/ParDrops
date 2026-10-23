package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminProductResponse;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.dto.ProductRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductImage;
import com.pardorps.pardrops.model.ProductVariant;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.ProductReviewRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio con la lógica de negocio del catálogo de productos.
 *
 * <p>Cubre tanto la consulta pública (listar y buscar por slug) como la gestión
 * del catálogo: el administrador puede modificar cualquier producto, mientras
 * que un vendedor solo puede tocar los productos que él mismo publica.</p>
 *
 * <p>Cada producto puede pertenecer a un vendedor (campo {@code seller}). Los
 * productos sin vendedor son los del catálogo propio de la plataforma y solo
 * puede administrarlos el rol ADMIN. El campo {@code active} permite retirar
 * un producto del catálogo sin borrarlo del historial de los pedidos.</p>
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final StoreRepository storeRepository;
    private final CategoryService categoryService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * @param productRepository      acceso a los productos.
     * @param productReviewRepository acceso a las reseñas (se borran con el producto).
     * @param storeRepository        acceso a las tiendas (para el slug público).
     * @param categoryService        validación de categorías asignables.
     * @param auditLogService        bitácora de auditoría.
     * @param notificationService    avisos dentro de la aplicación.
     */
    public ProductService(
            ProductRepository productRepository,
            ProductReviewRepository productReviewRepository,
            StoreRepository storeRepository,
            CategoryService categoryService,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.productRepository = productRepository;
        this.productReviewRepository = productReviewRepository;
        this.storeRepository = storeRepository;
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Devuelve los productos publicados del catálogo.
     *
     * @return lista de productos visibles para cualquier visitante.
     */
    @Transactional(readOnly = true)
    public List<Product> findPublished() {
        List<Product> products = productRepository.findPublished();
        enrich(products);
        return products;
    }

    /**
     * Devuelve todos los productos, incluidos los ocultos (uso del panel de
     * administración).
     *
     * @return lista completa de productos.
     */
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        List<Product> products = productRepository.findAllWithSeller();
        enrich(products);
        return products;
    }

    /**
     * Busca productos con los filtros del panel de administración.
     *
     * @param search   texto de búsqueda (nombre, slug o descripción).
     * @param sellerId filtra por vendedor propietario.
     * @param category filtra por categoría.
     * @param active   filtra por visibilidad.
     * @param page     número de página (empezando en 0).
     * @param size     tamaño de página.
     * @return página de productos con los datos del propietario.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> searchForAdmin(
            String search, Long sellerId, String category, Boolean active, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result = productRepository.searchForAdmin(
                trimToNull(search),
                sellerId,
                trimToNull(category),
                active,
                pageable);

        List<Product> products = result.getContent();
        enrich(products);

        List<AdminProductResponse> content = products.stream()
                .map(this::toAdminResponse)
                .toList();

        return PageResponse.of(content, result.getNumber(), result.getSize(),
                result.getTotalElements());
    }

    /**
     * Devuelve la ficha de un producto para el panel de administración.
     *
     * @param id identificador del producto.
     * @return el producto con los datos de su propietario.
     * @throws ApiException si el producto no existe (404).
     */
    @Transactional(readOnly = true)
    public AdminProductResponse findAdminDetail(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));
        enrich(List.of(product));
        return toAdminResponse(product);
    }

    /**
     * Devuelve los productos de un vendedor concreto.
     *
     * @param seller vendedor propietario.
     * @return lista de productos del vendedor.
     */
    @Transactional(readOnly = true)
    public List<Product> findBySeller(User seller) {
        List<Product> products = productRepository.findBySellerId(seller.getId());
        enrich(products);
        return products;
    }

    /**
     * Devuelve los productos publicados de una tienda concreta.
     *
     * <p>Es el catálogo que se muestra en la página pública de la tienda
     * ({@code /tiendas/{slug}}). Si la tienda está suspendida no se publica
     * nada: se responde como si no existiera.</p>
     *
     * @param slug slug público de la tienda.
     * @return lista de productos publicados de la tienda.
     * @throws ApiException si la tienda no existe o está suspendida (404).
     */
    @Transactional(readOnly = true)
    public List<Product> findPublishedByStoreSlug(String slug) {
        Store store = storeRepository.findBySlugWithOwner(slug)
                .orElseThrow(() -> ApiException.notFound("Tienda no encontrada: " + slug));

        if (store.getStatus() != StoreStatus.ACTIVE) {
            throw ApiException.notFound("Tienda no encontrada: " + slug);
        }

        List<Product> products =
                productRepository.findPublishedBySellerId(store.getSeller().getId());
        enrich(products);
        return products;
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
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + slug));
        enrich(List.of(product));
        return product;
    }

    /**
     * Crea un producto nuevo a partir de los datos recibidos.
     *
     * @param request datos del producto.
     * @param owner   vendedor propietario (nulo para los productos de la
     *                plataforma, que solo crea el administrador).
     * @return el producto creado.
     * @throws ApiException si el slug ya está en uso.
     */
    @Transactional
    public Product create(ProductRequest request, User owner) {
        if (productRepository.existsBySlug(request.slug())) {
            throw ApiException.conflict("Ya existe un producto con el slug: " + request.slug());
        }

        categoryService.validateAssignable(request.categories());

        Product product = new Product();
        applyRequest(product, request);
        product.setSeller(owner);
        Product saved = productRepository.save(product);
        enrich(List.of(saved));
        return saved;
    }

    /**
     * Actualiza un producto existente.
     *
     * @param id      identificador del producto.
     * @param request nuevos datos del producto.
     * @param actor   usuario que realiza la operación.
     * @return el producto actualizado.
     * @throws ApiException si el producto no existe, el slug choca con otro o
     *                     el actor no es el propietario del producto.
     */
    @Transactional
    public Product update(String id, ProductRequest request, User actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));

        requireOwnership(product, actor);

        if (productRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw ApiException.conflict("Ya existe otro producto con el slug: " + request.slug());
        }

        categoryService.validateAssignable(request.categories());

        applyRequest(product, request);
        Product saved = productRepository.save(product);
        enrich(List.of(saved));
        return saved;
    }

    /**
     * Elimina un producto del catálogo.
     *
     * @param id    identificador del producto.
     * @param actor usuario que realiza la operación.
     * @throws ApiException si el producto no existe o el actor no es su
     *                     propietario.
     */
    @Transactional
    public void delete(String id, User actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));

        requireOwnership(product, actor);

        // Las reseñas referencian al producto; se eliminan antes para no
        // violar la clave extranjera de la tabla de reseñas.
        productReviewRepository.deleteByProductId(id);
        productRepository.deleteById(id);
    }

    /**
     * Publica u oculta un producto del catálogo (moderación del administrador).
     *
     * <p>Al ocultar un producto se avisa al vendedor responsable y se deja
     * constancia en la bitácora de auditoría, porque se trata de una acción del
     * administrador que afecta a un tercero.</p>
     *
     * @param id     identificador del producto.
     * @param active {@code true} para publicarlo, {@code false} para ocultarlo.
     * @param admin  administrador que realiza la moderación (puede ser nulo en
     *               llamadas internas).
     * @return el producto con el nuevo estado.
     * @throws ApiException si el producto no existe.
     */
    @Transactional
    public Product setActive(String id, boolean active, User admin) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));

        boolean changed = product.isActive() != active;
        product.setActive(active);
        Product saved = productRepository.save(product);
        enrich(List.of(saved));

        if (changed) {
            recordModeration(saved, active, admin);
        }

        return saved;
    }

    /**
     * Registra la moderación del producto en la auditoría y avisa al vendedor.
     *
     * @param product producto moderado.
     * @param active  nuevo estado de visibilidad.
     * @param admin   administrador que realizó la acción.
     */
    private void recordModeration(Product product, boolean active, User admin) {
        auditLogService.record(admin, AuditAction.PRODUCT_MODERATED, "PRODUCT",
                product.getId(),
                (active ? "Producto publicado: " : "Producto oculto: ") + product.getName());

        User seller = product.getSeller();
        if (seller == null) {
            return;
        }

        notificationService.notify(
                seller,
                NotificationType.PRODUCT,
                active ? "Tu producto vuelve a estar publicado" : "Tu producto fue oculto",
                active
                        ? "\"" + product.getName() + "\" vuelve a aparecer en el catálogo."
                        : "El administrador ocultó \"" + product.getName()
                                + "\" del catálogo. Revisa sus datos y vuelve a publicarlo.",
                "/vendedor/productos");
    }

    /**
     * Devuelve un producto comprobando que el actor puede gestionarlo.
     *
     * @param id    identificador del producto.
     * @param actor usuario que intenta la operación.
     * @return el producto solicitado.
     * @throws ApiException si el producto no existe (404) o el actor no es su
     *                     propietario (403).
     */
    @Transactional(readOnly = true)
    public Product findOwned(String id, User actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Producto no encontrado: " + id));
        requireOwnership(product, actor);
        enrich(List.of(product));
        return product;
    }

    /**
     * Comprueba que el actor puede modificar el producto.
     *
     * <p>El administrador puede actuar sobre cualquier producto. Un vendedor
     * solo puede actuar sobre los productos que él mismo publica, y un
     * cliente no puede actuar sobre ninguno.</p>
     *
     * @param product producto afectado.
     * @param actor   usuario que intenta la operación.
     * @throws ApiException si el actor no tiene permiso (403).
     */
    private void requireOwnership(Product product, User actor) {
        if (actor == null) {
            throw ApiException.forbidden("Necesitas iniciar sesión para gestionar productos");
        }
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() != Role.SELLER) {
            throw ApiException.forbidden("No tienes permiso para gestionar productos");
        }
        if (product.getSeller() == null
                || !product.getSeller().getId().equals(actor.getId())) {
            throw ApiException.forbidden("Solo puedes gestionar tus propios productos");
        }
    }

    /**
     * Rellena los datos del vendedor y de su tienda en los productos.
     *
     * <p>Evita que el frontend tenga que resolver las relaciones: los campos
     * {@code sellerId}, {@code sellerName} y {@code storeSlug} llegan ya
     * calculados.</p>
     *
     * @param products productos a completar.
     */
    private void enrich(List<Product> products) {
        if (products.isEmpty()) {
            return;
        }

        Map<Long, String> storeSlugs = storeRepository.findAll().stream()
                .filter(store -> store.getSeller() != null)
                .collect(Collectors.toMap(
                        store -> store.getSeller().getId(),
                        Store::getSlug,
                        (first, second) -> first));

        for (Product product : products) {
            User seller = product.getSeller();
            if (seller == null) {
                product.setSellerId(null);
                product.setSellerName(null);
                product.setStoreSlug(null);
                continue;
            }
            product.setSellerId(seller.getId());
            product.setSellerName(seller.getFullName());
            product.setStoreSlug(storeSlugs.get(seller.getId()));
        }
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

    /**
     * Convierte un producto en su ficha para el panel de administración.
     *
     * <p>Incluye los datos del propietario (vendedor, su tienda y su estado) y
     * la URL de la imagen principal, que es lo que el administrador necesita
     * para moderar el catálogo global.</p>
     *
     * @param product producto a convertir.
     * @return la ficha lista para enviar al cliente.
     */
    private AdminProductResponse toAdminResponse(Product product) {
        User seller = product.getSeller();
        Store store = seller == null ? null : storeOf(seller.getId());

        String imageUrl = product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().stream()
                        .map(ProductImage::getUrl)
                        .findFirst()
                        .orElse(null));

        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStock(),
                product.isActive(),
                List.copyOf(product.getCategories()),
                imageUrl,
                seller == null ? null : seller.getId(),
                seller == null ? null : seller.getFullName(),
                seller == null ? null : seller.getEmail(),
                store == null ? null : store.getId(),
                store == null ? null : store.getName(),
                store == null ? null : store.getSlug(),
                store == null ? null : store.getStatus(),
                seller == null ? null : seller.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * Busca la tienda de un vendedor.
     *
     * @param sellerId identificador del vendedor.
     * @return la tienda, o {@code null} si el vendedor no tiene.
     */
    private Store storeOf(Long sellerId) {
        return storeRepository.findBySellerId(sellerId).orElse(null);
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
}
