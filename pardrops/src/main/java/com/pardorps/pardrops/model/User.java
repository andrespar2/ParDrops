package com.pardorps.pardrops.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entidad que representa a un usuario registrado en la plataforma.
 *
 * <p>Almacena las credenciales de acceso (correo y contraseña cifrada) y los
 * datos básicos de contacto. Los usuarios con rol {@link Role#ADMIN} pueden
 * gestionar el catálogo de productos.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /** Identificador interno autogenerado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo electrónico; actúa como nombre de usuario y es único. */
    @Column(nullable = false, length = 180)
    private String email;

    /** Contraseña almacenada como hash BCrypt (nunca en texto plano). */
    @Column(nullable = false)
    private String password;

    /** Nombre completo de la persona. */
    @Column(nullable = false, length = 120)
    private String fullName;

    /** Teléfono de contacto (opcional al registrarse). */
    @Column(length = 30)
    private String phone;

    /** Rol del usuario dentro de la plataforma. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Fecha y hora de creación de la cuenta. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
