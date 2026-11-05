package com.pardorps.pardrops.cache;

/**
 * Nomenclatura centralizada de las claves de caché.
 *
 * <p>Todas las claves llevan el prefijo {@code pardrops:} y una versión para
 * poder invalidar un contrato de datos sin borrar la caché a mano. Nunca se
 * incluyen datos personales en la clave: los identificadores se usan solo
 * cuando el valor es privado y la clave se invalida por usuario.</p>
 */
public final class CacheKeys {

    /** Prefijo raíz de todas las claves del proyecto. */
    public static final String ROOT = "pardrops:";

    /** Versión del contrato de productos públicos. */
    public static final String VERSION = "v1";

    public static final String PRODUCTS_PUBLISHED = ROOT + "productos:publicados:" + VERSION;
    public static final String PRODUCTS_HIGHLIGHTS = ROOT + "productos:destacados:" + VERSION;
    public static final String PRODUCTS_FEED = ROOT + "productos:feed:" + VERSION + ":";
    public static final String PRODUCTS_SEARCH = ROOT + "productos:admin:" + VERSION + ":";
    public static final String PRODUCTS_SELLER = ROOT + "productos:vendedor:" + VERSION + ":";
    public static final String PRODUCTS_STORE = ROOT + "productos:tienda:" + VERSION + ":";
    public static final String PRODUCTS_RANKING = ROOT + "productos:ranking:" + VERSION + ":";
    public static final String PRODUCT_DETAIL = ROOT + "productos:detalle:" + VERSION + ":";
    public static final String STORES_PUBLIC = ROOT + "tiendas:publicas:" + VERSION;
    public static final String STORES_ADMIN = ROOT + "tiendas:admin:" + VERSION + ":";
    public static final String STORES_SUMMARY = ROOT + "tiendas:resumen:" + VERSION + ":";
    public static final String REVIEWS_PRODUCT = ROOT + "resenas:producto:" + VERSION + ":";
    public static final String REVIEWS_SELLER = ROOT + "resenas:vendedor:" + VERSION + ":";
    public static final String CATEGORIES = ROOT + "categorias:" + VERSION;

    private CacheKeys() {
    }

    /**
     * Construye una clave de listado con filtros normalizados.
     *
     * @param base   prefijo lógico.
     * @param parts  partes del filtro, ya normalizadas.
     * @return clave completa y determinista.
     */
    public static String of(String base, Object... parts) {
        StringBuilder builder = new StringBuilder(base);
        for (Object part : parts) {
            builder.append(part == null ? "_" : String.valueOf(part)).append(':');
        }
        return builder.toString();
    }
}
