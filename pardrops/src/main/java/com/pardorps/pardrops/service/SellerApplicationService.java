package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.SellerApplicationRequest;
import com.pardorps.pardrops.dto.SellerApplicationResponse;
import com.pardorps.pardrops.dto.SellerApplicationReviewRequest;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SellerApplication;
import com.pardorps.pardrops.model.SellerApplicationStatus;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.SellerApplicationRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Servicio con la lógica de las solicitudes para convertirse en vendedor.
 *
 * <p>Cubre el envío de la solicitud por parte del cliente y su revisión por
 * parte del administrador. Al aprobar una solicitud, el usuario pasa a tener
 * el rol {@link Role#SELLER} y se le crea automáticamente una
 * {@link Store} con su nombre comercial; al rechazarla, se le notifica el
 * motivo y puede presentar una solicitud nueva.</p>
 */
@Service
public class SellerApplicationService {

    /** Tipo de entidad que se registra en la bitácora de auditoría. */
    private static final String ENTITY_TYPE = "SELLER_APPLICATION";

    private final SellerApplicationRepository sellerApplicationRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    /**
     * @param sellerApplicationRepository acceso a las solicitudes.
     * @param userRepository               acceso a los usuarios.
     * @param storeRepository              acceso a las tiendas.
     * @param storeService                 servicio de tiendas (generación de slug).
     * @param auditLogService              bitácora de auditoría.
     * @param notificationService          avisos dentro de la aplicación.
     */
    public SellerApplicationService(
            SellerApplicationRepository sellerApplicationRepository,
            UserRepository userRepository,
            StoreRepository storeRepository,
            StoreService storeService,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.sellerApplicationRepository = sellerApplicationRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.storeService = storeService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Registra la solicitud de un cliente para convertirse en vendedor.
     *
     * @param applicant cliente que solicita la mejora de rol.
     * @param request   datos del negocio.
     * @return la solicitud registrada, en estado pendiente.
     * @throws ApiException si el usuario ya es vendedor (409) o si tiene una
     *                     solicitud pendiente (409).
     */
    @Transactional
    public SellerApplicationResponse submit(User applicant, SellerApplicationRequest request) {
        if (applicant.getRole() == Role.SELLER) {
            throw ApiException.conflict("Ya eres vendedor en ParDrops");
        }

        SellerApplication pending = sellerApplicationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(applicant.getId())
                .orElse(null);

        if (pending != null && pending.getStatus() == SellerApplicationStatus.PENDING) {
            throw ApiException.conflict(
                    "Ya tienes una solicitud en revisión. Espera a que el administrador la revise");
        }

        SellerApplication application = new SellerApplication();
        application.setUser(applicant);
        application.setBusinessName(request.businessName().trim());
        application.setTaxId(trimToNull(request.taxId()));
        application.setDescription(trimToNull(request.description()));
        application.setPhone(trimToNull(request.phone()));
        application.setCity(trimToNull(request.city()));
        application.setStatus(SellerApplicationStatus.PENDING);
        application.setCreatedAt(Instant.now());

        SellerApplication saved = sellerApplicationRepository.save(application);
        notificationService.notify(
                applicant,
                NotificationType.SELLER_APPLICATION,
                "Solicitud registrada",
                "Recibimos tu solicitud para vender en ParDrops. Te avisaremos cuando sea revisada.",
                "/vender");
        return toResponse(saved);
    }

    /**
     * Devuelve la última solicitud enviada por un usuario.
     *
     * @param user usuario autenticado.
     * @return la solicitud, o {@code null} si nunca ha enviado ninguna.
     */
    @Transactional(readOnly = true)
    public SellerApplicationResponse findMine(User user) {
        return sellerApplicationRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Devuelve todas las solicitudes registradas.
     *
     * @return listado de solicitudes, de la más reciente a la más antigua.
     */
    @Transactional(readOnly = true)
    public List<SellerApplicationResponse> findAll() {
        return sellerApplicationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve las solicitudes pendientes de revisión.
     *
     * @return listado de solicitudes pendientes.
     */
    @Transactional(readOnly = true)
    public List<SellerApplicationResponse> findPending() {
        return sellerApplicationRepository
                .findByStatusOrderByCreatedAtDesc(SellerApplicationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Aprueba o rechaza una solicitud de vendedor.
     *
     * <p>Al aprobar, el cliente pasa a ser {@link Role#SELLER} y se le crea su
     * tienda. En ambos casos se deja constancia en la bitácora de auditoría y
     * se le envía una notificación con el resultado.</p>
     *
     * @param applicationId identificador de la solicitud.
     * @param admin         administrador que realiza la revisión.
     * @param review        decisión tomada.
     * @return la solicitud revisada.
     * @throws ApiException si la solicitud no existe (404), si ya fue revisada
     *                     (409) o si el rechazo no lleva motivo (400).
     */
    @Transactional
    public SellerApplicationResponse review(
            Long applicationId, User admin, SellerApplicationReviewRequest review) {
        SellerApplication application = sellerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> ApiException.notFound(
                        "Solicitud no encontrada: " + applicationId));

        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw ApiException.conflict(
                    "La solicitud ya fue revisada (estado actual: " + application.getStatus() + ")");
        }

        User applicant = application.getUser();
        boolean approve = Boolean.TRUE.equals(review.approve());
        String notes = trimToNull(review.reviewNotes());

        if (!approve && notes == null) {
            throw ApiException.badRequest(
                    "Debes indicar el motivo por el que rechazas la solicitud");
        }

        application.setReviewNotes(notes);
        application.setReviewedBy(admin);
        application.setReviewedAt(Instant.now());

        if (approve) {
            application.setStatus(SellerApplicationStatus.APPROVED);
            promoteToSeller(applicant, application);
            auditLogService.record(admin, AuditAction.SELLER_APPLICATION_APPROVED,
                    ENTITY_TYPE, String.valueOf(applicationId),
                    "Solicitud aprobada para " + application.getBusinessName());
            notificationService.notify(
                    applicant,
                    NotificationType.SELLER_APPLICATION,
                    "¡Tu solicitud fue aprobada!",
                    "Ya puedes publicar productos en tu tienda de ParDrops.",
                    "/vendedor");
        } else {
            application.setStatus(SellerApplicationStatus.REJECTED);
            auditLogService.record(admin, AuditAction.SELLER_APPLICATION_REJECTED,
                    ENTITY_TYPE, String.valueOf(applicationId),
                    "Solicitud rechazada: " + notes);
            notificationService.notify(
                    applicant,
                    NotificationType.SELLER_APPLICATION,
                    "Tu solicitud no fue aprobada",
                    notes,
                    "/vender");
        }

        return toResponse(sellerApplicationRepository.save(application));
    }

    /**
     * Convierte al usuario en vendedor y le crea su tienda.
     *
     * @param applicant   usuario que ha sido aprobado.
     * @param application solicitud aprobada.
     */
    private void promoteToSeller(User applicant, SellerApplication application) {
        applicant.setRole(Role.SELLER);
        userRepository.save(applicant);

        if (storeRepository.findBySellerId(applicant.getId()).isPresent()) {
            return;
        }

        Store store = new Store();
        store.setSeller(applicant);
        store.setName(application.getBusinessName());
        store.setSlug(storeService.buildSlug(application.getBusinessName()));
        store.setDescription(application.getDescription());
        store.setContactEmail(applicant.getEmail());
        store.setContactPhone(
                application.getPhone() != null ? application.getPhone() : applicant.getPhone());
        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);
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

    /**
     * Convierte una entidad de solicitud en su representación pública.
     *
     * @param application solicitud a convertir.
     * @return la solicitud lista para enviar al cliente.
     */
    private SellerApplicationResponse toResponse(SellerApplication application) {
        User applicant = application.getUser();
        User reviewer = application.getReviewedBy();
        return new SellerApplicationResponse(
                application.getId(),
                application.getBusinessName(),
                application.getTaxId(),
                application.getDescription(),
                application.getPhone(),
                application.getCity(),
                application.getStatus(),
                application.getReviewNotes(),
                applicant != null ? applicant.getFullName() : null,
                applicant != null ? applicant.getEmail() : null,
                reviewer != null ? reviewer.getFullName() : null,
                application.getReviewedAt(),
                application.getCreatedAt()
        );
    }
}
