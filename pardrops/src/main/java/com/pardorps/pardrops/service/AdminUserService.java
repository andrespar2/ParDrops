package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AdminSellerResponse;
import com.pardorps.pardrops.dto.AdminUserDetailResponse;
import com.pardorps.pardrops.dto.AdminUserResponse;
import com.pardorps.pardrops.dto.PageResponse;
import com.pardorps.pardrops.dto.UserStatusRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SellerBalance;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio con la lógica de administración de cuentas de usuario.
 *
 * <p>Permite listar y buscar cuentas, consultar la ficha completa de cada una
 * (con su tienda y su actividad) y suspender o reactivar su acceso. Al
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
    private final ProductRepository productRepository;
    private final OrderSubOrderRepository subOrderRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SellerBalanceRepository balanceRepository;

    /**
     * @param userRepository       acceso a los usuarios.
     * @param storeRepository      acceso a las tiendas (para mostrar la del vendedor).
     * @param productRepository    acceso al catálogo (para contar productos).
     * @param subOrderRepository   acceso a las partes de pedido (para las ventas).
     * @param auditLogService      bitácora de auditoría.
     * @param notificationService  avisos dentro de la aplicación.
     * @param balanceRepository    saldo y comisiones del vendedor.
     */
    public AdminUserService(
            UserRepository userRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            OrderSubOrderRepository subOrderRepository,
            AuditLogService auditLogService,
            NotificationService notificationService,
            SellerBalanceRepository balanceRepository
    ) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.subOrderRepository = subOrderRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.balanceRepository = balanceRepository;
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
     * Busca cuentas con los filtros y la paginación del panel.
     *
     * @param search texto de búsqueda (correo, nombre o teléfono).
     * @param role   filtra por rol.
     * @param status filtra por estado.
     * @param page   número de página (empezando en 0).
     * @param size   tamaño de página.
     * @return página de cuentas.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> searchPage(
            String search, Role role, UserStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.ASC, "fullName"));

        Page<User> result = userRepository.searchForAdmin(
                trimToNull(search), role, status, pageable);

        Map<Long, String> storeNames = storeNamesIndex();
        List<AdminUserResponse> content = result.getContent().stream()
                .map(user -> toResponse(user, storeNames))
                .toList();

        return PageResponse.of(content, result.getNumber(), result.getSize(),
                result.getTotalElements());
    }

    /**
     * Devuelve la ficha completa de una cuenta.
     *
     * <p>Incluye los datos de la tienda del vendedor y sus cifras de actividad
     * (productos publicados y ventas), que es lo que el panel muestra en la
     * sección de usuarios.</p>
     *
     * @param userId identificador de la cuenta.
     * @return la ficha de la cuenta.
     * @throws ApiException si la cuenta no existe (404).
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResponse findDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado: " + userId));

        Store store = storeRepository.findBySellerId(user.getId()).orElse(null);

        return new AdminUserDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                store == null ? null : store.getId(),
                store == null ? null : store.getName(),
                store == null ? null : store.getSlug(),
                store == null ? null : store.getStatus(),
                productRepository.countBySellerId(user.getId()),
                subOrderRepository.countBySellerId(user.getId()),
                salesTotal(user.getId())
        );
    }

    /**
     * Devuelve el total vendido por un vendedor tratando el nulo de la suma.
     *
     * <p>La consulta JPQL devuelve {@code null} cuando el vendedor todavía no
     * tiene ventas, así que se normaliza a cero.</p>
     *
     * @param sellerId identificador del vendedor.
     * @return el total vendido, cero si no hay ventas.
     */
    private Double salesTotal(Long sellerId) {
        Double total = subOrderRepository.sumTotalBySellerId(sellerId);
        return total == null ? 0.0 : total;
    }

    /**
     * Devuelve el listado de vendedores con su tienda y su actividad.
     *
     * @param storeStatus filtra por estado de la tienda (puede ser nulo).
     * @param search      texto de búsqueda por nombre, correo o tienda.
     * @return listado de vendedores ordenado por nombre.
     */
    @Transactional(readOnly = true)
    public List<AdminSellerResponse> findSellers(StoreStatus storeStatus, String search) {
        List<Store> stores = storeStatus == null
                ? storeRepository.findAllWithOwner()
                : storeRepository.findByStatusWithOwner(storeStatus);

        String term = search == null ? null : search.trim().toLowerCase();

        return stores.stream()
                .filter(store -> matches(store, term))
                .map(this::toSellerResponse)
                .toList();
    }

    /**
     * Comprueba si una tienda coincide con el texto de búsqueda.
     *
     * @param store tienda a comprobar.
     * @param term  texto ya normalizado (puede ser nulo).
     * @return {@code true} si coincide.
     */
    private boolean matches(Store store, String term) {
        if (term == null || term.isEmpty()) {
            return true;
        }
        User seller = store.getSeller();
        return contains(store.getName(), term)
                || contains(seller.getFullName(), term)
                || contains(seller.getEmail(), term)
                || contains(store.getSlug(), term);
    }

    /**
     * Comprueba si un texto contiene el término indicado.
     *
     * @param value texto de origen (puede ser nulo).
     * @param term  término ya normalizado.
     * @return {@code true} si lo contiene.
     */
    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase().contains(term);
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

    /**
     * Convierte una tienda en la ficha de su vendedor para el panel.
     *
     * @param store tienda (con su vendedor cargado).
     * @return la ficha del vendedor lista para enviar al cliente.
     */
    private AdminSellerResponse toSellerResponse(Store store) {
        User seller = store.getSeller();
        SellerBalance balance = balanceRepository.findBySellerId(seller.getId()).orElse(null);
        return new AdminSellerResponse(
                seller.getId(),
                seller.getFullName(),
                seller.getEmail(),
                seller.getPhone(),
                seller.getStatus(),
                store.getId(),
                store.getName(),
                store.getSlug(),
                store.getStatus(),
                productRepository.countBySellerId(seller.getId()),
                subOrderRepository.countBySellerId(seller.getId()),
                subOrderRepository.sumTotalBySellerId(seller.getId()),
                balance == null ? 0.0 : balance.getCommissionTotal(),
                balance == null ? 0.0 : balance.getPendingAmount(),
                balance == null ? 0.0 : balance.getAvailableAmount(),
                seller.getCreatedAt()
        );
    }

    /**
     * Normaliza un texto opcional: recorta los espacios y convierte la cadena
     * vacía en {@code null}.
     *
     * @param value texto de origen.
     * @return el texto normalizado o {@code null}.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
