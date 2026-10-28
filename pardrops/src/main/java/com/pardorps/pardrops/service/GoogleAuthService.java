package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.GoogleLoginException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce una identidad verificada de Google en una cuenta de ParDrops.
 *
 * <p>El flujo es idempotente:</p>
 *
 * <ol>
 *   <li>Si ya existe una cuenta vinculada al identificador {@code sub}, se
 *       reutiliza y se actualizan el nombre y el avatar.</li>
 *   <li>Si existe una cuenta local con el mismo correo y Google confirma que el
 *       correo está verificado, la cuenta se vincula.</li>
 *   <li>Si no existe ninguna, se crea un cliente nuevo con una contraseña
 *       técnica aleatoria que nadie conoce.</li>
 * </ol>
 */
@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * @param userRepository  acceso a las cuentas.
     * @param passwordEncoder cifrado de la contraseña técnica.
     * @param auditLogService bitácora de auditoría.
     */
    public GoogleAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * Devuelve la cuenta asociada a una identidad de Google, creándola o
     * vinculándola si es necesario.
     *
     * @param oidcUser identidad devuelta por Google.
     * @return la cuenta de ParDrops.
     * @throws GoogleLoginException si Google no aporta el correo o si la cuenta
     *                              no puede entrar.
     */
    @Transactional
    public User resolveUser(OidcUser oidcUser) {
        Map<String, Object> claims = oidcUser.getClaims();
        String email = text(claims.get("email")).toLowerCase(Locale.ROOT);
        String providerId = text(claims.get("sub"));

        if (email.isBlank() || providerId.isBlank()) {
            throw new GoogleLoginException(
                    "sin-correo",
                    "Google no devolvió un correo utilizable");
        }

        String fullName = text(claims.get("name"));
        if (fullName.isBlank()) {
            fullName = email.substring(0, email.indexOf('@'));
        }
        String avatarUrl = text(claims.get("picture"));
        boolean emailVerified = isTrue(claims.get("email_verified"));

        Optional<User> linked = userRepository.findByAuthProviderAndProviderId(
                AuthProvider.GOOGLE, providerId);
        if (linked.isPresent()) {
            User user = linked.get();
            ensureActive(user);
            return userRepository.save(updateProfile(user, fullName, avatarUrl));
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            ensureActive(user);
            if (!emailVerified) {
                throw new GoogleLoginException(
                        "correo-no-verificado",
                        "Google no confirmó la propiedad del correo");
            }
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setProviderId(providerId);
            User saved = userRepository.save(updateProfile(user, fullName, avatarUrl));
            auditLogService.record(
                    saved,
                    AuditAction.GOOGLE_ACCOUNT_LINKED,
                    "USER",
                    String.valueOf(saved.getId()),
                    "Cuenta local vinculada con Google");
            log.info("Cuenta {} vinculada con Google", saved.getId());
            return saved;
        }

        User created = User.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName.trim())
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .avatarUrl(blankToNull(avatarUrl))
                .createdAt(Instant.now())
                .build();
        User saved = userRepository.save(created);
        auditLogService.record(
                saved,
                AuditAction.GOOGLE_ACCOUNT_CREATED,
                "USER",
                String.valueOf(saved.getId()),
                "Cuenta creada con Google");
        log.info("Cuenta {} creada con Google", saved.getId());
        return saved;
    }

    /**
     * Actualiza los datos que Google puede haber cambiado.
     *
     * @param user      cuenta a actualizar.
     * @param fullName  nombre visible.
     * @param avatarUrl foto de perfil.
     * @return la misma cuenta actualizada.
     */
    private User updateProfile(User user, String fullName, String avatarUrl) {
        if (!fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (!avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        return user;
    }

    /**
     * Rechaza el acceso de una cuenta suspendida antes de emitir el JWT.
     *
     * @param user cuenta a comprobar.
     */
    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new GoogleLoginException(
                    "cuenta-suspendida",
                    "La cuenta está suspendida");
        }
    }

    /** Convierte un valor de claim en texto seguro. */
    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /** Interpreta el claim booleano que puede llegar como texto. */
    private boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(text(value));
    }

    /** Devuelve nulo para cadenas vacías. */
    private String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
