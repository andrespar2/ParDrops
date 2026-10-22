package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AuditLogResponse;
import com.pardorps.pardrops.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de la bitácora de auditoría.
 *
 * <p>Expone los últimos registros de decisiones de administración. Solo
 * accesible con rol ADMIN.</p>
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    /**
     * @param auditLogService servicio de auditoría.
     */
    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Devuelve los registros de auditoría más recientes.
     *
     * @return 200 con el listado de registros.
     */
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> list() {
        return ResponseEntity.ok(auditLogService.findRecent());
    }
}
