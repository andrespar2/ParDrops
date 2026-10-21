package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.ProductImage;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Siembra datos iniciales la primera vez que arranca la aplicación.
 *
 * <p>Crea la cuenta de administrador por defecto y carga el catálogo de
 * ejemplo, siempre y cuando la base de datos esté vacía. Así el proyecto es
 * utilizable nada más arrancarlo, sin pasos manuales.</p>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    /** Correo de la cuenta de administrador inicial. */
    private static final String ADMIN_EMAIL = "admin@pardrops.com";

    /** Contraseña de la cuenta de administrador inicial (solo para desarrollo). */
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository    acceso a los usuarios.
     * @param productRepository acceso a los productos.
     * @param passwordEncoder   cifrado de contraseñas.
     */
    public DataSeeder(
            UserRepository userRepository,
            ProductRepository productRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
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
                        119_900, null, 50, 4.8, 124, true, true, false,
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
                        239_900, 319_900.0, 30, 4.6, 89, false, true, true,
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
                        319_900, null, 40, 4.9, 256, true, true, false,
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
                        199_900, null, 25, 4.7, 78, false, false, false,
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
                        99_900, null, 60, 4.5, 45, true, false, false,
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
                        359_900, 439_900.0, 15, 4.8, 112, false, true, true,
                        List.of("Ropa", "Chaquetas"),
                        List.of("bomber", "negra", "minimalista"),
                        List.of(image("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800&h=800&fit=crop", "Cazadora bomber negra", true))
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
}
