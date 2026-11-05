package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.cache.CacheService;
import com.pardorps.pardrops.cache.CacheStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de la caché de servidor para el panel de administración.
 *
 * <p>No sustituye a una herramienta de observabilidad: solo expone los
 * contadores internos de aciertos, fallos, errores e invalidaciones para poder
 * comprobar el efecto real de Redis.</p>
 */
@RestController
@RequestMapping("/api/admin/cache")
public class AdminCacheController {

    private final CacheService cacheService;

    public AdminCacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * @return contadores actuales de la caché.
     */
    @GetMapping("/stats")
    public ResponseEntity<CacheStats> stats() {
        return ResponseEntity.ok(cacheService.stats());
    }
}
