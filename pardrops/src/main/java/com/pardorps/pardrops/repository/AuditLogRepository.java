package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a la bitácora de auditoría.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Devuelve los registros de auditoría más recientes.
     *
     * @param limit número máximo de registros a devolver.
     * @return listado de registros, del más reciente al más antiguo.
     */
    List<AuditLog> findTop50ByOrderByCreatedAtDesc(int limit);
}
