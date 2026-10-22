package com.pardorps.pardrops.config;

import com.pardorps.pardrops.storage.MediaStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracion de los recursos estaticos servidos por la API.
 *
 * <p>Las imagenes subidas se guardan fuera de la base de datos y se sirven
 * como recursos publicos desde la ruta configurada (por defecto
 * {@code /media}), de modo que se puedan mostrar en el catalogo y en los
 * pedidos.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final MediaStorage mediaStorage;

    /**
     * @param mediaStorage almacen de las imagenes subidas.
     */
    public WebConfig(MediaStorage mediaStorage) {
        this.mediaStorage = mediaStorage;
    }

    /**
     * Publica las imagenes guardadas en el almacen.
     *
     * @param registry registro de manejadores de recursos.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(mediaStorage.basePublicPath() + "/**")
                .addResourceLocations(mediaStorage.storageLocation());
    }
}
