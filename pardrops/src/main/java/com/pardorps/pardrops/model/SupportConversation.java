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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Conversación de soporte entre un usuario y la administración.
 *
 * <p>Cada conversación pertenece a un único usuario; el personal autorizado
 * puede verla y responderla. Se guardan marcas de no leído para cada lado.</p>
 */
@Entity
@Table(name = "support_conversations")
@Getter
@Setter
public class SupportConversation {

    /** Identificador. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Usuario que abrió la conversación. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Asunto breve. */
    @Column(nullable = false, length = 160)
    private String subject;

    /** Estado de la conversación. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupportStatus status = SupportStatus.OPEN;

    /** Hay mensajes nuevos para el usuario. */
    @Column(name = "user_unread", nullable = false)
    private boolean userUnread = false;

    /** Hay mensajes nuevos para el equipo de soporte. */
    @Column(name = "staff_unread", nullable = false)
    private boolean staffUnread = true;

    /** Fecha de apertura. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Fecha del último mensaje. */
    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    /** Ajusta las fechas antes de guardar. */
    @PrePersist
    @PreUpdate
    void touch() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastMessageAt == null) {
            lastMessageAt = createdAt;
        }
    }
}
