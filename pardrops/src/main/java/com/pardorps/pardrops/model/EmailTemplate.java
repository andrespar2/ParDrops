package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Plantilla de un correo transaccional.
 *
 * <p>El asunto y el cuerpo son editables desde el panel de administración. El
 * cuerpo admite HTML y variables con la forma {@code {{variable}}}. Las
 * plantillas predeterminadas se crean al arrancar y nunca se sobrescriben.</p>
 */
@Entity
@Table(
        name = "email_templates",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_email_templates_code",
                columnNames = "code")
)
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplate {

    /** Identificador autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de correo al que pertenece la plantilla. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmailTemplateCode code;

    /** Nombre visible en el panel de administración. */
    @Column(nullable = false, length = 120)
    private String name;

    /** Explicación breve de cuándo se envía. */
    @Column(length = 400)
    private String description;

    /** Asunto del correo. */
    @Column(nullable = false, length = 250)
    private String subject;

    /** Cuerpo HTML con variables. */
    @Column(nullable = false, length = 20000)
    private String bodyHtml;

    /** Indica si la plantilla se usa al enviar el correo. */
    @Column(nullable = false)
    private boolean active = true;

    /** Fecha de creación. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Fecha de la última modificación. */
    private Instant updatedAt;

    /** Rellena las fechas al crear la plantilla. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Actualiza la fecha de modificación. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
