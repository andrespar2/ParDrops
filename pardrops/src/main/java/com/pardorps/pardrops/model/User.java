package com.pardorps.pardrops.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
 * gestionar el catálogo de productos y todo el marketplace; los
 * {@link Role#SELLER} gestionan su tienda y sus pedidos; el resto son
 * clientes.</p>
 *
 * <p>El campo {@code status} se añadió con la evolución a marketplace: las
 * cuentas existentes se marcan como {@link UserStatus#ACTIVE} mediante la
 * migración {@code MarketplaceSchemaMigration}.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_provider", columnNames = {"auth_provider", "provider_id"})
        }
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
    @JsonIgnore
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

    /**
     * Estado de la cuenta (activa o suspendida).
     *
     * <p>Se declara sin {@code nullable = false} a propósito: la columna se
     * añadió a una base de datos ya poblada y Hibernate la crea sin valor por
     * defecto. La migración {@code MarketplaceSchemaMigration} rellena los
     * valores nulos y {@link #onCreate()} garantiza que toda cuenta nueva
     * nazca activa.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Origen de la autenticación de la cuenta.
     *
     * <p>Las cuentas anteriores al acceso con Google se marcan como
     * {@link AuthProvider#LOCAL} mediante {@code GoogleSchemaMigration}. Las
     * cuentas creadas o vinculadas con Google usan
     * {@link AuthProvider#GOOGLE}.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Identificador de la cuenta en el proveedor externo.
     *
     * <p>Para Google se guarda el valor de {@code sub}, que identifica la
     * identidad aunque el correo cambie. Es nulo en las cuentas locales.</p>
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /** URL de la foto de perfil que devuelve el proveedor externo. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Fecha y hora de creación de la cuenta. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacío requerido por JPA (lo genera {@code @NoArgsConstructor}). */

    /** Rellena los valores por defecto antes de insertar la cuenta. */
    @PrePersist
    void onCreate() {
        if (status == null) {
            status = UserStatus.ACTIVE;
        }
        if (authProvider == null) {
            authProvider = AuthProvider.LOCAL;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Indica si la cuenta está activa.
     *
     * <p>Las cuentas antiguas que aún no tengan valor en la columna se
     * consideran activas.</p>
     *
     * @return {@code true} si la cuenta puede operar en la plataforma.
     */
    public boolean isActive() {
        return status == null || status == UserStatus.ACTIVE;
    }
}
