package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Mensaje de una conversación de soporte.
 *
 * <p>El autor puede ser el usuario o un administrador; {@code fromStaff}
 * permite distinguirlos al pintar la conversación.</p>
 */
@Entity
@Table(name = "support_messages")
@Getter
@Setter
public class SupportMessage {

    /** Identificador. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Conversación a la que pertenece. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private SupportConversation conversation;

    /** Autor del mensaje. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    /** Nombre mostrado del autor. */
    @Column(name = "author_name", length = 120)
    private String authorName;

    /** Indica si lo escribió el equipo de soporte. */
    @Column(name = "from_staff", nullable = false)
    private boolean fromStaff;

    /** Contenido del mensaje. */
    @Column(nullable = false, length = 2000)
    private String body;

    /** Fecha de envío. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Ajusta la fecha antes de guardar. */
    @PrePersist
    void touch() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
