package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductImage;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Siembra datos iniciales la primera vez que arranca la aplicación.
 *
 * <p>Crea la cuenta de administrador por defecto, una cuenta de vendedor de
 * ejemplo con su tienda y carga el catálogo de ejemplo, siempre y cuando la
 * base de datos esté vacía. Así el proyecto es utilizable nada más arrancarlo,
 * sin pasos manuales.</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataSeeder implements CommandLineRunner {

    /** Correo de la cuenta de administrador inicial. */
    private static final String ADMIN_EMAIL = "admin@pardrops.com";

    /** Contraseña de la cuenta de administrador inicial (solo para desarrollo). */
    private static final String ADMIN_PASSWORD = "admin123";

    /** Correo de la cuenta de vendedor de ejemplo. */
    private static final String SELLER_EMAIL = "vendedor@pardrops.com";

    /** Contraseña de la cuenta de vendedor de ejemplo (solo para desarrollo). */
    private static final String SELLER_PASSWORD = "vendedor123";

    /** Nombre comercial de la tienda de ejemplo. */
    private static final String SELLER_STORE_NAME = "Estilo Urbano";

    /** Slug público de la tienda de ejemplo. */
    private static final String SELLER_STORE_SLUG = "estilo-urbano";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository    acceso a los usuarios.
     * @param productRepository acceso a los productos.
     * @param storeRepository   acceso a las tiendas.
     * @param passwordEncoder   cifrado de contraseñas.
     */
    public DataSeeder(
            UserRepository userRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ejecuta la carga de datos al arrancar.
     *
     * @param args argumentos de línea de comandos (no se usan).
     */
    @Override
    public void run(String... args) {
        seedAdmin();
        seedProducts();
        seedSeller();
    }

    /** Crea la cuenta de administrador si todavía no existe. */
    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName("Administrador ParDrops")
                .phone("+57 300 000 0000")
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .build();
        userRepository.save(admin);
    }

    /** Carga el catálogo de ejemplo solo si aún no hay productos. */
    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }
        productRepository.saveAll(List.of(
                product(
                        "prod-001",
                        "Camiseta Minimalista Negra",
                        "camiseta-minimalista-negra",
                        "Camiseta básica de algodón orgánico con diseño minimalista.",
                        "Camiseta confeccionada en algodón orgánico de alta calidad. Diseño minimalista con corte moderno y unisex. Ideal para looks casuales o sofisticados.",
                        129000, null, 50, 4.8, 124, true, true, false,
                        List.of("Ropa", "Camisetas"),
                        List.of("minimalista", "negra", "algodón"),
                        List.of(image("https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&h=800&fit=crop", "Camiseta minimalista negra", true),
                                image("https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800&h=800&fit=crop", "Camiseta minimalista negra - vista lateral", false))
                ),
                product(
                        "prod-002",
                        "Pantalón Cargo Slim",
                        "pantalon-cargo-slim",
                        "Pantalón cargo de corte slim con múltiples bolsillos.",
                        "Pantalón cargo moderno con corte slim fit. Confeccionado en mezcla de algodón y elastano para mayor comodidad. Múltiples bolsillos funcionales con diseño minimalista.",
                        258000, 344000.0, 30, 4.6, 89, false, true, true,
                        List.of("Ropa", "Pantalones"),
                        List.of("cargo", "slim", "negro"),
                        List.of(image("https://images.unsplash.com/photo-1552374196-1ab2a1c593e8?w=800&h=800&fit=crop", "Pantalón cargo slim", true))
                ),
                product(
                        "prod-003",
                        "Zapatillas Urbanas Blancas",
                        "zapatillas-urbanas-blancas",
                        "Zapatillas urbanas minimalistas en color blanco.",
                        "Zapatillas de estilo urbano minimalista. Suela de goma resistente y exterior de lona premium. Diseño versátil que combina con cualquier look.",
                        344000, null, 40, 4.9, 256, true, true, false,
                        List.of("Calzado", "Zapatillas"),
                        List.of("zapatillas", "urbanas", "blancas"),
                        List.of(image("https://images.unsplash.com/photo-1549298916-b41d501d3772?w=800&h=800&fit=crop", "Zapatillas urbanas blancas", true),
                                image("https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=800&h=800&fit=crop", "Zapatillas urbanas blancas - detalle", false))
                ),
                product(
                        "prod-004",
                        "Mochila Minimalista",
                        "mochila-minimalista",
                        "Mochila minimalista con compartimento para portátil.",
                        "Mochila de diseño minimalista con capacidad para portátil de 15\". Fabricada en nylon resistente al agua con acolchado ergonómico en espalda y correas.",
                        215000, null, 25, 4.7, 78, false, false, false,
                        List.of("Accesorios", "Mochilas"),
                        List.of("mochila", "minimalista", "portátil"),
                        List.of(image("https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&h=800&fit=crop", "Mochila minimalista", true))
                ),
                product(
                        "prod-005",
                        "Bufanda de Lana",
                        "bufanda-de-lana",
                        "Bufanda de lana merino suave al tacto.",
                        "Bufanda confeccionada en 100% lana merino. Suave, cálida y ligera. Diseño minimalista unisex disponible en varios colores.",
                        107500, null, 60, 4.5, 45, true, false, false,
                        List.of("Accesorios", "Bufandas"),
                        List.of("bufanda", "lana", "invierno"),
                        List.of(image("https://images.unsplash.com/photo-1605236453806-6ff36851218e?w=800&h=800&fit=crop", "Bufanda de lana", true))
                ),
                product(
                        "prod-006",
                        "Cazadora Bomber Negra",
                        "cazadora-bomber-negra",
                        "Cazadora bomber minimalista con forro interior.",
                        "Cazadora bomber de diseño minimalista. Exterior resistente al viento con forro acolchado. Bolsillos frontales y ribetes elásticos en puños y cintura.",
                        387000, 473000.0, 15, 4.8, 112, false, true, true,
                        List.of("Ropa", "Chaquetas"),
                        List.of("bomber", "negra", "minimalista"),
                        List.of(image("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&h=800&fit=crop", "Cazadora bomber negra", true))
                )
        ));
    }

    /**
     * Crea la cuenta de vendedor de ejemplo con su tienda y su catálogo.
     *
     * <p>Es idempotente: si el vendedor, la tienda o sus productos ya existen
     * no se vuelven a crear. Permite probar el flujo multivendedor (tienda
     * pública, catálogo del vendedor) sin tener que esperar a que un
     * administrador apruebe una solicitud real.</p>
     */
    private void seedSeller() {
        User seller = userRepository.findByEmail(SELLER_EMAIL).orElse(null);
        if (seller == null) {
            seller = User.builder()
                    .email(SELLER_EMAIL)
                    .password(passwordEncoder.encode(SELLER_PASSWORD))
                    .fullName("Vendedor de ejemplo")
                    .phone("+57 300 111 2222")
                    .role(Role.SELLER)
                    .createdAt(Instant.now())
                    .build();
            seller = userRepository.save(seller);
        }

        if (!storeRepository.existsBySlug(SELLER_STORE_SLUG)) {
            Store store = new Store();
            store.setSeller(seller);
            store.setName(SELLER_STORE_NAME);
            store.setSlug(SELLER_STORE_SLUG);
            store.setDescription("Accesorios urbanos y ropa con diseño minimalista.");
            store.setContactEmail(SELLER_EMAIL);
            store.setContactPhone("+57 300 111 2222");
            store.setStatus(StoreStatus.ACTIVE);
            storeRepository.save(store);
        }

        if (!productRepository.findBySellerId(seller.getId()).isEmpty()) {
            return;
        }

        productRepository.saveAll(List.of(
                sellerProduct(
                        seller,
                        "sld-001",
                        "Gorra Urbana Negra",
                        "gorra-urbana-negra",
                        "Gorra de panel con cierre regulable.",
                        "Gorra urbana de ala curva con corona de seis paneles. Cierre regulable en la parte trasera y bordado en la frente.",
                        89000, 120000.0, 40, 4.4, 32, false, true, false,
                        List.of("Accesorios", "Gorras"),
                        List.of("gorra", "urbana", "negra"),
                        List.of(image("https://images.unsplash.com/photo-1521369909029-2afed882baee?w=800&h=800&fit=crop", "Gorra urbana negra", true))
                ),
                sellerProduct(
                        seller,
                        "sld-002",
                        "Sudadera Oversized Gris",
                        "sudadera-oversized-gris",
                        "Sudadera oversized de algodón con capucha.",
                        "Sudadera oversized de algodón peinado con capucha doble y bolsillo frontal tipo canguro. Corte amplio y costuras reforzadas.",
                        165000, null, 25, 4.7, 58, true, false, false,
                        List.of("Ropa", "Sudadera"),
                        List.of("sudadera", "oversized", "gris"),
                        List.of(image("https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&h=800&fit=crop", "Sudadera oversized gris", true))
                ),
                sellerProduct(
                        seller,
                        "sld-003",
                        "Botella Térmica 500ml",
                        "botella-termica-500ml",
                        "Botella reutilizable de acero inoxidable.",
                        "Botella térmica de acero inoxidable de 500 ml con acabado mate. Conserva la temperatura durante 12 horas y es apta para agua y bebidas carbonatadas.",
                        75000, 95000.0, 60, 4.6, 71, false, false, true,
                        List.of("Accesorios", "Botellas"),
                        List.of("botella", "térmica", "acero"),
                        List.of(image("https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=800&h=800&fit=crop", "Botella térmica de acero inoxidable", true))
                )
        ));
    }

    /**
     * Crea una imagen embebida.
     *
     * @param url       URL de la imagen.
     * @param alt       texto alternativo.
     * @param isPrimary si es la principal.
     * @return la imagen construida.
     */
    private ProductImage image(String url, String alt, boolean isPrimary) {
        return new ProductImage(url, alt, isPrimary);
    }

    /**
     * Construye un producto del catálogo de ejemplo.
     *
     * @return el producto listo para guardar.
     */
    private Product product(
            String id, String name, String slug, String description, String longDescription,
            double price, Double originalPrice, int stock, Double rating, Integer reviewCount,
            boolean isNew, boolean isFeatured, boolean onSale,
            List<String> categories, List<String> tags, List<ProductImage> images
    ) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug(slug);
        product.setDescription(description);
        product.setLongDescription(longDescription);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setCurrency("COP");
        product.setStock(stock);
        product.setRating(rating);
        product.setReviewCount(reviewCount);
        product.setNew(isNew);
        product.setFeatured(isFeatured);
        product.setOnSale(onSale);
        product.getCategories().addAll(categories);
        product.getTags().addAll(tags);
        product.getImages().addAll(images);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return product;
    }

    /**
     * Construye un producto del catálogo de ejemplo perteneciente al vendedor indicado.
     *
     * @param seller vendedor propietario del producto.
     * @return el producto listo para guardar.
     */
    private Product sellerProduct(
            User seller,
            String id, String name, String slug, String description, String longDescription,
            double price, Double originalPrice, int stock, Double rating, Integer reviewCount,
            boolean isNew, boolean isFeatured, boolean onSale,
            List<String> categories, List<String> tags, List<ProductImage> images
    ) {
        Product product = product(
                id, name, slug, description, longDescription,
                price, originalPrice, stock, rating, reviewCount,
                isNew, isFeatured, onSale, categories, tags, images);
        product.setSeller(seller);
        return product;
    }
}
