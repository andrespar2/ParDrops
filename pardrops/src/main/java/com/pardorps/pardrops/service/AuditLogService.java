package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AuditLogResponse;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.AuditLog;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio que registra y consulta la bitácora de auditoría.
 *
 * <p>Se invoca desde los servicios de administración cada vez que se toma una
 * decisión que afecta a la cuenta de otro usuario (aprobar o rechazar una
 * solicitud de vendedor, suspender una cuenta, moderar un producto).</p>
 */
@Service
public class AuditLogService {

    /** Número máximo de registros que devuelve la consulta al panel. */
    private static final int MAX_ENTRIES = 50;

    private final AuditLogRepository auditLogRepository;

    /**
     * @param auditLogRepository acceso a la bitácora de auditoría.
     */
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Registra una acción de administración.
     *
     * @param actor      usuario que ejecuta la acción (puede ser nulo).
     * @param action     tipo de acción.
     * @param entityType tipo de entidad afectada.
     * @param entityId   identificador de la entidad afectada.
     * @param details    detalle legible de la acción.
     */
    @Transactional
    public void record(User actor, AuditAction action, String entityType, String entityId, String details) {
        AuditLog entry = new AuditLog();
        entry.setActor(actor);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details);
        auditLogRepository.save(entry);
    }

    /**
     * Devuelve los registros de auditoría más recientes.
     *
     * @return listado de registros, del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> findRecent() {
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc(MAX_ENTRIES)
                .stream()
                .map(entry -> new AuditLogResponse(
                        entry.getId(),
                        entry.getActor() != null ? entry.getActor().getFullName() : "Sistema",
                        entry.getAction(),
                        entry.getEntityType(),
                        entry.getEntityId(),
                        entry.getDetails(),
                        entry.getCreatedAt()
                ))
                .toList();
    }
}
