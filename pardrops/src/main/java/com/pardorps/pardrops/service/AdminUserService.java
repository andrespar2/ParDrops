package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminUserResponse;
import com.pardorps.pardrops.dto.UserStatusRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio con la lógica de administración de cuentas de usuario.
 *
 * <p>Permite listar y buscar cuentas, y suspender o reactivar su acceso. Al
 * suspender una cuenta se deja constancia en la bitácora de auditoría y se
 * avisa al usuario; la cuenta conserva sus datos y puede reactivarse en
 * cualquier momento.</p>
 */
@Service
public class AdminUserService {

    /** Tipo de entidad que se registra en la bitácora de auditoría. */
    private static final String ENTITY_TYPE = "USER";

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * @param userRepository       acceso a los usuarios.
     * @param storeRepository      acceso a las tiendas (para mostrar la del vendedor).
     * @param auditLogService      bitácora de auditoría.
     * @param notificationService  avisos dentro de la aplicación.
     */
    public AdminUserService(
            UserRepository userRepository,
            StoreRepository storeRepository,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Devuelve todas las cuentas de la plataforma.
     *
     * @return listado de cuentas.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        return toResponses(userRepository.findAll());
    }

    /**
     * Busca cuentas por correo, nombre o teléfono.
     *
     * @param term texto de búsqueda; si está vacío se devuelven todas.
     * @return listado de cuentas que coinciden con el filtro.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> search(String term) {
        if (term == null || term.isBlank()) {
            return findAll();
        }
        return toResponses(userRepository.search(term.trim()));
    }

    /**
     * Suspende o reactiva el acceso de una cuenta.
     *
     * @param userId  identificador de la cuenta.
     * @param admin   administrador que realiza el cambio.
     * @param request nuevo estado y motivo del cambio.
     * @return la cuenta con el nuevo estado.
     * @throws ApiException si la cuenta no existe (404) o si se intenta
     *                     suspender al propio administrador (409).
     */
    @Transactional
    public AdminUserResponse changeStatus(Long userId, User admin, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado: " + userId));

        if (admin != null && admin.getId().equals(user.getId())
                && request.status() == UserStatus.SUSPENDED) {
            throw ApiException.conflict("No puedes suspender tu propia cuenta");
        }

        boolean suspending = request.status() == UserStatus.SUSPENDED;
        user.setStatus(request.status());
        User saved = userRepository.save(user);

        if (suspending) {
            suspendStore(user);
            auditLogService.record(admin, AuditAction.USER_SUSPENDED, ENTITY_TYPE,
                    String.valueOf(userId),
                    "Cuenta suspendida: " + (request.reason() == null ? "sin motivo" : request.reason()));
            notificationService.notify(
                    user, NotificationType.ACCOUNT, "Tu cuenta fue suspendida",
                    request.reason() == null
                            ? "El administrador suspendió temporalmente tu acceso."
                            : request.reason(),
                    null);
        } else {
            auditLogService.record(admin, AuditAction.USER_REACTIVATED, ENTITY_TYPE,
                    String.valueOf(userId), "Cuenta reactivada");
            notificationService.notify(
                    user, NotificationType.ACCOUNT, "Tu cuenta fue reactivada",
                    "Ya puedes volver a usar ParDrops con normalidad.",
                    null);
        }

        return toResponse(saved);
    }

    /**
     * Suspende la tienda del vendedor cuando su cuenta se suspende.
     *
     * @param user usuario cuya cuenta se acaba de suspender.
     */
    private void suspendStore(User user) {
        if (user.getRole() != Role.SELLER) {
            return;
        }
        storeRepository.findBySellerId(user.getId()).ifPresent(store -> {
            store.setStatus(StoreStatus.SUSPENDED);
            storeRepository.save(store);
        });
    }

    /**
     * Convierte una lista de usuarios en su representación para el panel.
     *
     * @param users usuarios a convertir.
     * @return listado de cuentas.
     */
    private List<AdminUserResponse> toResponses(List<User> users) {
        Map<Long, String> storeNames = storeNamesIndex();
        return users.stream()
                .map(user -> toResponse(user, storeNames))
                .toList();
    }

    /**
     * Convierte un usuario en su representación para el panel.
     *
     * @param user usuario a convertir.
     * @return la cuenta lista para enviar al cliente.
     */
    private AdminUserResponse toResponse(User user) {
        return toResponse(user, storeNamesIndex());
    }

    /**
     * Indexa el nombre de cada tienda por el identificador de su vendedor.
     *
     * @return mapa de identificador de vendedor a nombre de tienda.
     */
    private Map<Long, String> storeNamesIndex() {
        return storeRepository.findAll().stream()
                .filter(store -> store.getSeller() != null)
                .collect(Collectors.toMap(
                        store -> store.getSeller().getId(),
                        Store::getName,
                        (first, second) -> first));
    }

    /**
     * Convierte un usuario en su representación para el panel.
     *
     * @param user       usuario a convertir.
     * @param storeNames nombres de tienda indexados por vendedor.
     * @return la cuenta lista para enviar al cliente.
     */
    private AdminUserResponse toResponse(User user, Map<Long, String> storeNames) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                storeNames.get(user.getId()),
                user.getCreatedAt()
        );
    }
}
