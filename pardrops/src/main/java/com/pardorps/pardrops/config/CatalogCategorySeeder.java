package com.pardorps.pardrops.config;

import com.pardorps.pardrops.model.Category;
import com.pardorps.pardrops.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Siembra el árbol de categorías del marketplace general.
 *
 * <p>Crea las 19 categorías principales, sus subcategorías y las colecciones
 * comerciales. Es idempotente: solo crea las que faltan y no altera el nombre
 * ni la descripción de las existentes, de modo que las ediciones del
 * administrador se conservan.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CatalogCategorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogCategorySeeder.class);

    private final CategoryRepository categoryRepository;

    /**
     * @param categoryRepository acceso a las categorías.
     */
    public CatalogCategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** Definición de una categoría a sembrar. */
    private record Seed(String slug, String name, String parentSlug, int order, boolean featured) {
    }

    private static final List<Seed> MAIN = List.of(
            new Seed("ropa", "Ropa y moda", null, 1, true),
            new Seed("calzado", "Calzado", null, 2, true),
            new Seed("tecnologia", "Tecnología y electrónica", null, 3, true),
            new Seed("streaming", "Streaming", null, 4, false),
            new Seed("licencias-digitales", "Licencias y productos digitales", null, 5, false),
            new Seed("hogar-muebles", "Hogar y muebles", null, 6, true),
            new Seed("electrodomesticos", "Electrodomésticos", null, 7, false),
            new Seed("belleza-cuidado", "Belleza y cuidado personal", null, 8, false),
            new Seed("perfumeria", "Perfumería", null, 9, false),
            new Seed("deportes-fitness", "Deportes y fitness", null, 10, true),
            new Seed("mascotas", "Mascotas", null, 11, false),
            new Seed("juguetes-videojuegos", "Juguetes, videojuegos y entretenimiento", null, 12, false),
            new Seed("ferreteria-herramientas", "Ferretería y herramientas", null, 13, false),
            new Seed("papeleria-oficina", "Papelería y oficina", null, 14, false),
            new Seed("alimentos-mercado", "Alimentos y variedades de mercado", null, 15, false),
            new Seed("vehiculos", "Automóviles y motocicletas", null, 16, false),
            new Seed("joyeria-relojeria", "Joyería y relojería", null, 17, false),
            new Seed("bebes-ninos", "Bebés y niños", null, 18, false),
            new Seed("variedades", "De todo / Variedades", null, 19, true)
    );

    private static final List<Seed> SUBCATEGORIES = List.of(
            new Seed("ropa-hombre", "Ropa de hombre", "ropa-y-moda", 1, false),
            new Seed("ropa-mujer", "Ropa de mujer", "ropa-y-moda", 2, false),
            new Seed("ropa-ninos", "Ropa de niños", "ropa-y-moda", 3, false),
            new Seed("camisetas", "Camisetas", "ropa-y-moda", 4, false),
            new Seed("conjuntos", "Conjuntos", "ropa-y-moda", 5, false),
            new Seed("accesorios", "Accesorios", "ropa-y-moda", 6, false),
            new Seed("zapatos", "Zapatos", "calzado", 1, false),
            new Seed("tenis", "Tenis", "calzado", 2, false),
            new Seed("sandalias", "Sandalias", "calzado", 3, false),
            new Seed("botas", "Botas", "calzado", 4, false),
            new Seed("celulares", "Celulares", "tecnologia", 1, false),
            new Seed("computadores", "Computadores", "tecnologia", 2, false),
            new Seed("tablets", "Tablets", "tecnologia", 3, false),
            new Seed("televisores", "Televisores", "tecnologia", 4, false),
            new Seed("audifonos", "Audífonos", "tecnologia", 5, false),
            new Seed("componentes", "Componentes y periféricos", "tecnologia", 6, false),
            new Seed("suscripciones", "Suscripciones", "streaming", 1, false),
            new Seed("software", "Licencias de software", "licencias-digitales", 1, false),
            new Seed("videojuegos-digitales", "Videojuegos y tarjetas", "licencias-digitales", 2, false),
            new Seed("decoracion", "Decoración", "hogar-muebles", 1, false),
            new Seed("muebles", "Muebles", "hogar-muebles", 2, false),
            new Seed("utensilios", "Utensilios y organización", "hogar-muebles", 3, false),
            new Seed("neveras", "Neveras", "electrodomesticos", 1, false),
            new Seed("lavadoras", "Lavadoras", "electrodomesticos", 2, false),
            new Seed("pequenos-electrodomesticos", "Pequeños electrodomésticos", "electrodomesticos", 3, false),
            new Seed("maquillaje", "Maquillaje", "belleza-cuidado", 1, false),
            new Seed("barberia", "Barbería y peluquería", "belleza-cuidado", 2, false),
            new Seed("perfumes", "Perfumes", "perfumeria", 1, false),
            new Seed("implementos-deportivos", "Implementos deportivos", "deportes-fitness", 1, false),
            new Seed("ropa-deportiva", "Ropa deportiva", "deportes-fitness", 2, false),
            new Seed("alimentos-mascotas", "Alimentos para mascotas", "mascotas", 1, false),
            new Seed("juguetes", "Juguetes", "juguetes-videojuegos", 1, false),
            new Seed("consolas", "Consolas y videojuegos", "juguetes-videojuegos", 2, false),
            new Seed("herramientas", "Herramientas", "ferreteria-herramientas", 1, false),
            new Seed("materiales", "Materiales de construcción", "ferreteria-herramientas", 2, false),
            new Seed("utiles-escolares", "Útiles escolares", "papeleria-oficina", 1, false),
            new Seed("oficina", "Materiales de oficina", "papeleria-oficina", 2, false),
            new Seed("dulceria", "Dulcería", "alimentos-mercado", 1, false),
            new Seed("importados", "Productos importados", "alimentos-mercado", 2, false),
            new Seed("accesorios-vehiculos", "Accesorios para vehículos", "vehiculos", 1, false),
            new Seed("repuestos", "Repuestos", "vehiculos", 2, false),
            new Seed("relojes", "Relojes", "joyeria-relojeria", 1, false),
            new Seed("joyas", "Joyas", "joyeria-relojeria", 2, false),
            new Seed("articulos-bebe", "Artículos para bebé", "bebes-ninos", 1, false),
            new Seed("juguetes-ninos", "Juguetes infantiles", "bebes-ninos", 2, false)
    );

    private static final List<Seed> COLLECTIONS = List.of(
            new Seed("ofertas", "Ofertas", null, 1, true),
            new Seed("novedades", "Productos nuevos", null, 2, true),
            new Seed("liquidacion", "Liquidación", null, 3, false),
            new Seed("mas-vendidos", "Más vendidos", null, 4, true),
            new Seed("destacados", "Destacados", null, 5, true),
            new Seed("temporada-navidena", "Navidad y temporada", null, 6, false)
    );

    @Override
    public void run(String... args) {
        int created = 0;
        for (Seed seed : MAIN) {
            created += seedCategory(seed, false);
        }
        for (Seed seed : SUBCATEGORIES) {
            created += seedCategory(seed, false);
        }
        for (Seed seed : COLLECTIONS) {
            created += seedCategory(seed, true);
        }

        if (created > 0) {
            log.info("Categorías del marketplace creadas: {}", created);
        }
    }

    /**
     * Crea la categoría si no existe.
     *
     * @param seed       definición.
     * @param collection indica si es una colección.
     * @return 1 si se creó, 0 si ya existía.
     */
    private int seedCategory(Seed seed, boolean collection) {
        Category category = categoryRepository.findBySlug(seed.slug()).orElse(null);
        if (category != null) {
            return 0;
        }

        Category parent = seed.parentSlug() == null
                ? null
                : categoryRepository.findBySlug(seed.parentSlug()).orElse(null);

        Category created = new Category();
        created.setName(seed.name());
        created.setSlug(seed.slug());
        created.setActive(true);
        created.setFeatured(seed.featured());
        created.setSortOrder(seed.order());
        created.setCollection(collection);
        created.setParent(parent);
        categoryRepository.save(created);
        return 1;
    }
}
