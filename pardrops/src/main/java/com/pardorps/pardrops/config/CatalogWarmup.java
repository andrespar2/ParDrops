package com.pardorps.pardrops.config;

import com.pardorps.pardrops.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Precalienta el catálogo publicado después de arrancar.
 *
 * <p>La primera consulta del feed carga todas las colecciones de los productos.
 * Hacerlo en segundo plano evita que el primer visitante pague ese coste y deja
 * la conexión con la base de datos preparada. Un fallo de precalentamiento no
 * afecta al arranque ni a la disponibilidad de la API.</p>
 */
@Component
public class CatalogWarmup {

    private static final Logger log = LoggerFactory.getLogger(CatalogWarmup.class);

    private final ProductService productService;

    public CatalogWarmup(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Lanza la carga en un hilo aparte para no bloquear el arranque.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCatalog() {
        CompletableFuture.runAsync(() -> {
            try {
                int size = productService.findPublished().size();
                log.info("Catálogo precalentado: {} productos publicados", size);
            } catch (RuntimeException exception) {
                log.warn("No se pudo precalentar el catálogo: {}", exception.getMessage());
            }
        });
    }
}
