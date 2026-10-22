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
 * Aviso generado dentro de la aplicación para un usuario concreto.
 *
 * <p>Las notificaciones se muestran en la campana de la cabecera. Cada usuario
 * solo puede leer las suyas: la API filtra siempre por el usuario
 * autenticado, nunca por un identificador recibido en la petición.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    /** Identificador autogenerado del aviso. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario destinatario del aviso. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Tipo del aviso. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** Título corto del aviso. */
    @Column(nullable = false, length = 120)
    private String title;

    /** Texto explicativo del aviso. */
    @Column(nullable = false, length = 500)
    private String message;

    /** Ruta interna de la aplicación a la que lleva el aviso (opcional). */
    @Column(length = 200)
    private String link;

    /** Indica si el usuario ya leyó el aviso. */
    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** Fecha y hora de creación del aviso. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA. */
    public Notification() {
    }

    /** Asigna la fecha de creación antes de insertar el aviso. */
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
