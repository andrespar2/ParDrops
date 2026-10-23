package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro de auditoría de las acciones de administración relevantes.
 *
 * <p>Guarda quién ejecutó la acción, sobre qué entidad y con qué detalle. Es
 * de solo lectura: se rellena desde los servicios y se consulta en el panel de
 * administración.</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog {

    /** Identificador autogenerado del registro. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que ejecutó la acción (nulo en acciones del sistema). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** Tipo de acción realizada. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    /** Tipo de entidad afectada (por ejemplo, {@code SELLER_APPLICATION}). */
    @Column(nullable = false, length = 60)
    private String entityType;

    /** Identificador de la entidad afectada. */
    @Column(length = 60)
    private String entityId;

    /** Detalle legible de la acción. */
    @Column(length = 500)
    private String details;

    /** Fecha y hora de la acción. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public AuditLog() {
    }

    /** Asigna la fecha de la acción antes de insertar el registro. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
