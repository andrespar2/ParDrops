package com.pardorps.pardrops.config;

import com.pardorps.pardrops.storage.FileSystemMediaStorage;
import com.pardorps.pardrops.storage.MediaStorage;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
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
        // En producción el contenido vive en PostgreSQL y lo sirve
        // MediaFileController; aquí solo se publica el almacén en disco.
        if (mediaStorage instanceof FileSystemMediaStorage fileSystemStorage) {
            registry.addResourceHandler(fileSystemStorage.basePublicPath() + "/**")
                    .addResourceLocations(fileSystemStorage.storageLocation())
                    // Los nombres son UUID versionados: el contenido de una
                    // misma URL no cambia, así que puede cachearse un año.
                    .setCacheControl(org.springframework.http.CacheControl
                            .maxAge(java.time.Duration.ofDays(365))
                            .cachePublic()
                            .immutable());
        }
    }

    /**
     * Añade ETag a las respuestas públicas de catálogo y tiendas para que el
     * navegador pueda revalidar con {@code If-None-Match} y recibir 304.
     *
     * @return registro del filtro limitado a endpoints públicos.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> publicEtagFilter() {
        ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/products/*", "/api/stores/*", "/api/categories");
        registration.setName("publicEtagFilter");
        return registration;
    }
}
