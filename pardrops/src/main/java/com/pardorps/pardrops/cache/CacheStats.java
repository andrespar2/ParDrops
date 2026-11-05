package com.pardorps.pardrops.cache;

/**
 * Contadores de uso de la capa de caché.
 *
 * <p>Se exponen en el panel de administración para poder medir el impacto real
 * de Redis sin depender de herramientas externas de monitorización.</p>
 *
 * @param hits        lecturas resueltas desde caché.
 * @param misses      lecturas que han tenido que ir a la base de datos.
 * @param puts        escrituras guardadas en caché.
 * @param evictions   claves invalidadas tras una modificación.
 * @param errors      operaciones que han fallado (Redis caído o sin permisos).
 * @param hitRatio    porcentaje de aciertos (0 a 1).
 * @param redisEnabled indica si el adaptador de Redis está activo.
 * @param available   indica si Redis respondió en la última comprobación.
 * @param storedKeys  número de claves propias conocidas, cuando es medible.
 */
public record CacheStats(
        long hits,
        long misses,
        long puts,
        long evictions,
        long errors,
        double hitRatio,
        boolean redisEnabled,
        boolean available,
        long storedKeys
) {
}
