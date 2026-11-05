package com.pardorps.pardrops.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de la caché de servidor.
 *
 * <p>La implementación debe ser tolerante a fallos: si Redis no está
 * disponible, {@link #get} devuelve vacío y {@link #put} no hace nada. La base
 * de datos sigue siendo la fuente de verdad.</p>
 */
public interface CacheService {

    /**
     * Recupera un valor serializado.
     *
     * @param key  clave completa.
     * @param type tipo esperado.
     * @param <T>  tipo del valor.
     * @return el valor si existe y es válido.
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Recupera una lista serializada.
     *
     * @param key        clave completa.
     * @param elementType tipo de cada elemento.
     * @param <T>        tipo del elemento.
     * @return la lista si existe y es válida.
     */
    <T> Optional<List<T>> getList(String key, Class<T> elementType);

    /**
     * Guarda un valor con expiración.
     *
     * @param key   clave completa.
     * @param value valor serializable.
     * @param ttl   tiempo de vida.
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Invalida claves concretas.
     *
     * @param keys claves completas.
     */
    void evict(String... keys);

    /**
     * Invalida todas las claves de un prefijo lógico.
     *
     * @param prefix prefijo completo, por ejemplo {@code pardrops:productos:}.
     */
    void evictByPrefix(String prefix);

    /**
     * @return una foto de los contadores actuales.
     */
    CacheStats stats();
}
