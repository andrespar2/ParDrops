package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.EmailTemplateRequest;
import com.pardorps.pardrops.dto.EmailTemplateResponse;
import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.mail.EmailTemplateDefaults;
import com.pardorps.pardrops.mail.EmailTemplateRenderer;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.EmailTemplate;
import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.EmailTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestiona las plantillas de correo y su renderizado.
 */
@Service
public class EmailTemplateService {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateService.class);

    private static final String ENTITY_TYPE = "EMAIL_TEMPLATE";

    private final EmailTemplateRepository repository;
    private final AuditLogService auditLogService;

    /**
     * @param repository      acceso a las plantillas.
     * @param auditLogService bitácora de auditoría.
     */
    public EmailTemplateService(
            EmailTemplateRepository repository,
            AuditLogService auditLogService
    ) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    /**
     * Devuelve todas las plantillas.
     *
     * @return listado ordenado por código.
     */
    @Transactional(readOnly = true)
    public List<EmailTemplateResponse> findAll() {
        return repository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Devuelve una plantilla por su código.
     *
     * @param code código del correo.
     * @return la plantilla.
     * @throws ApiException si no existe.
     */
    @Transactional(readOnly = true)
    public EmailTemplateResponse findByCode(EmailTemplateCode code) {
        return toResponse(getRequired(code));
    }

    /**
     * Actualiza el asunto y el cuerpo de una plantilla.
     *
     * @param actor   administrador que realiza el cambio.
     * @param code    código del correo.
     * @param request nuevos valores.
     * @return la plantilla actualizada.
     */
    @Transactional
    public EmailTemplateResponse update(User actor, EmailTemplateCode code, EmailTemplateRequest request) {
        EmailTemplate template = getRequired(code);
        template.setSubject(request.subject().trim());
        template.setBodyHtml(request.bodyHtml());
        if (request.active() != null) {
            template.setActive(request.active());
        }
        EmailTemplate saved = repository.save(template);
        auditLogService.record(
                actor,
                AuditAction.EMAIL_TEMPLATE_UPDATED,
                ENTITY_TYPE,
                code.name(),
                "Plantilla de correo actualizada: " + code.name());
        log.info("Plantilla de correo actualizada: {}", code);
        return toResponse(saved);
    }

    /**
     * Restaura los valores predeterminados de una plantilla.
     *
     * @param actor administrador que realiza el cambio.
     * @param code  código del correo.
     * @return la plantilla restaurada.
     */
    @Transactional
    public EmailTemplateResponse reset(User actor, EmailTemplateCode code) {
        EmailTemplate template = getRequired(code);
        EmailTemplateDefaults.DefaultTemplate defaults = EmailTemplateDefaults.of(code);
        template.setSubject(defaults.subject());
        template.setBodyHtml(defaults.bodyHtml());
        template.setActive(true);
        EmailTemplate saved = repository.save(template);
        auditLogService.record(
                actor,
                AuditAction.EMAIL_TEMPLATE_RESET,
                ENTITY_TYPE,
                code.name(),
                "Plantilla de correo restaurada: " + code.name());
        log.info("Plantilla de correo restaurada: {}", code);
        return toResponse(saved);
    }

    /**
     * Crea las plantillas predeterminadas que falten.
     *
     * <p>Es idempotente y no sobrescribe las plantillas ya editadas.</p>
     */
    @Transactional
    public void seedDefaults() {
        for (EmailTemplateDefaults.DefaultTemplate defaults : EmailTemplateDefaults.all()) {
            if (repository.existsByCode(defaults.code())) {
                continue;
            }
            EmailTemplate template = new EmailTemplate();
            template.setCode(defaults.code());
            template.setName(defaults.name());
            template.setDescription(defaults.description());
            template.setSubject(defaults.subject());
            template.setBodyHtml(defaults.bodyHtml());
            template.setActive(true);
            repository.save(template);
            log.info("Plantilla de correo creada: {}", defaults.code());
        }
    }

    /**
     * Renderiza una plantilla activa con los valores indicados.
     *
     * @param code  código del correo.
     * @param model valores de las variables.
     * @return el correo renderizado o vacío si la plantilla está inactiva.
     */
    @Transactional(readOnly = true)
    public Optional<RenderedEmail> renderIfActive(EmailTemplateCode code, Map<String, ?> model) {
        return repository.findByCode(code)
                .filter(EmailTemplate::isActive)
                .map(template -> EmailTemplateRenderer.render(
                        template.getSubject(), template.getBodyHtml(), model));
    }

    /**
     * Obtiene la plantilla o lanza un 404.
     *
     * @param code código del correo.
     * @return la plantilla.
     */
    private EmailTemplate getRequired(EmailTemplateCode code) {
        return repository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("No existe la plantilla " + code));
    }

    /**
     * Convierte la entidad en su representación pública.
     *
     * @param template plantilla.
     * @return la respuesta.
     */
    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getCode(),
                template.getName(),
                template.getDescription(),
                template.getSubject(),
                template.getBodyHtml(),
                template.isActive(),
                template.getUpdatedAt());
    }
}
