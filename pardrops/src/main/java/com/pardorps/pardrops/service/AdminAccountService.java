package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminAccountResponse;
import com.pardorps.pardrops.dto.CreateAdminRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AdminLevel;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Gestión de cuentas administrativas.
 *
 * <p>El propietario ({@link AdminLevel#OWNER}) es único y solo él puede crear,
 * cambiar de nivel o desactivar administradores. Las reglas se aplican en el
 * servidor: ocultar botones en la interfaz no es suficiente.</p>
 */
@Service
public class AdminAccountService {

    /** Tipo de entidad usado en la auditoría. */
    private static final String ENTITY_TYPE = "ADMIN_ACCOUNT";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    /**
     * @param userRepository   acceso a las cuentas.
     * @param passwordEncoder  cifrado de contraseñas.
     * @param authService      usuario autenticado.
     * @param auditLogService  registro de auditoría.
     */
    public AdminAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    /**
     * Lista las cuentas administrativas.
     *
     * @return cuentas ordenadas por fecha de alta.
     */
    @Transactional(readOnly = true)
    public List<AdminAccountResponse> list() {
        requireStaff();
        return admins().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Crea una cuenta administrativa.
     *
     * @param request datos de la cuenta.
     * @return la cuenta creada.
     */
    @Transactional
    public AdminAccountResponse create(CreateAdminRequest request) {
        User actor = requireOwner();

        if (request.level() == AdminLevel.OWNER) {
            throw ApiException.badRequest("Solo puede existir un administrador principal");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.badRequest("Ya existe una cuenta con ese correo");
        }

        User admin = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .adminLevel(request.level())
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(admin);
        auditLogService.record(actor, AuditAction.ADMIN_CREATED, ENTITY_TYPE,
                String.valueOf(saved.getId()), "Nivel " + saved.getAdminLevel());
        return toResponse(saved);
    }

    /**
     * Cambia el nivel de una cuenta administrativa.
     *
     * @param id    identificador de la cuenta.
     * @param level nuevo nivel.
     * @return la cuenta actualizada.
     */
    @Transactional
    public AdminAccountResponse updateLevel(Long id, AdminLevel level) {
        User actor = requireOwner();
        User target = findAdmin(id);

        if (target.getId().equals(actor.getId())) {
            throw ApiException.badRequest("No puedes cambiar tu propio nivel");
        }
        if (level == AdminLevel.OWNER) {
            throw ApiException.badRequest("Solo puede existir un administrador principal");
        }
        if (target.getAdminLevel() == AdminLevel.OWNER) {
            throw ApiException.badRequest("El administrador principal no puede ser degradado");
        }

        target.setAdminLevel(level);
        User saved = userRepository.save(target);
        auditLogService.record(actor, AuditAction.ADMIN_LEVEL_CHANGED, ENTITY_TYPE,
                String.valueOf(saved.getId()), "Nuevo nivel " + level);
        return toResponse(saved);
    }

    /**
     * Activa o desactiva una cuenta administrativa.
     *
     * @param id     identificador de la cuenta.
     * @param active {@code true} para reactivar.
     * @return la cuenta actualizada.
     */
    @Transactional
    public AdminAccountResponse setStatus(Long id, boolean active) {
        User actor = requireOwner();
        User target = findAdmin(id);

        if (target.getId().equals(actor.getId())) {
            throw ApiException.badRequest("No puedes desactivar tu propia cuenta");
        }
        if (!active && target.getAdminLevel() == AdminLevel.OWNER) {
            throw ApiException.badRequest("El administrador principal no puede desactivarse");
        }

        target.setStatus(active ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
        User saved = userRepository.save(target);
        auditLogService.record(actor,
                active ? AuditAction.ADMIN_REACTIVATED : AuditAction.ADMIN_DEACTIVATED,
                ENTITY_TYPE, String.valueOf(saved.getId()), null);
        return toResponse(saved);
    }

    /**
     * Comprueba si el usuario indicado es el propietario activo.
     *
     * @param user usuario a comprobar.
     * @return {@code true} si es administrador principal activo.
     */
    public boolean isOwner(User user) {
        return user != null
                && user.getRole() == Role.ADMIN
                && user.getAdminLevel() == AdminLevel.OWNER
                && user.getStatus() == UserStatus.ACTIVE;
    }

    /** Exige un administrador activo (propietario o gestor). */
    private void requireStaff() {
        User actor = authService.getCurrentUser();
        if (actor.getRole() != Role.ADMIN || actor.getAdminLevel() == null) {
            throw ApiException.forbidden("No tienes permisos de administración");
        }
    }

    /** Exige el administrador principal. */
    private User requireOwner() {
        User actor = authService.getCurrentUser();
        if (!isOwner(actor)) {
            throw ApiException.forbidden("Solo el administrador principal puede gestionar administradores");
        }
        return actor;
    }

    /** Busca una cuenta administrativa por identificador. */
    private User findAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La cuenta no existe"));
        if (user.getRole() != Role.ADMIN) {
            throw ApiException.badRequest("La cuenta no es administrativa");
        }
        return user;
    }

    /** Cuentas con rol ADMIN ordenadas por alta. */
    private List<User> admins() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .sorted(Comparator.comparing(User::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Convierte la cuenta en su representación pública. */
    private AdminAccountResponse toResponse(User user) {
        return new AdminAccountResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAdminLevel(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
