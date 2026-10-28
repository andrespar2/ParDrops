package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.EmailTemplateRequest;
import com.pardorps.pardrops.dto.EmailTemplateResponse;
import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.mail.EmailPreviewData;
import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.EmailTemplateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de administración de las plantillas de correo.
 *
 * <p>Todas las rutas quedan protegidas por la regla {@code /api/admin/**} de
 * {@code SecurityConfig}: solo un administrador puede consultarlas o editarlas.</p>
 */
@RestController
@RequestMapping("/api/admin/email-templates")
public class AdminEmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final AuthService authService;

    /**
     * @param emailTemplateService servicio de plantillas.
     * @param authService          usuario autenticado para la auditoría.
     */
    public AdminEmailTemplateController(
            EmailTemplateService emailTemplateService,
            AuthService authService
    ) {
        this.emailTemplateService = emailTemplateService;
        this.authService = authService;
    }

    /**
     * Devuelve todas las plantillas.
     *
     * @return listado de plantillas.
     */
    @GetMapping
    public List<EmailTemplateResponse> findAll() {
        return emailTemplateService.findAll();
    }

    /**
     * Devuelve una plantilla por su código.
     *
     * @param code código del correo.
     * @return la plantilla.
     */
    @GetMapping("/{code}")
    public EmailTemplateResponse find(@PathVariable EmailTemplateCode code) {
        return emailTemplateService.findByCode(code);
    }

    /**
     * Actualiza una plantilla.
     *
     * @param code    código del correo.
     * @param request nuevos valores.
     * @return la plantilla actualizada.
     */
    @PutMapping("/{code}")
    public EmailTemplateResponse update(
            @PathVariable EmailTemplateCode code,
            @Valid @RequestBody EmailTemplateRequest request
    ) {
        User admin = authService.getCurrentUser();
        return emailTemplateService.update(admin, code, request);
    }

    /**
     * Restaura los valores predeterminados de una plantilla.
     *
     * @param code código del correo.
     * @return la plantilla restaurada.
     */
    @PostMapping("/{code}/reset")
    public EmailTemplateResponse reset(@PathVariable EmailTemplateCode code) {
        User admin = authService.getCurrentUser();
        return emailTemplateService.reset(admin, code);
    }

    /**
     * Genera una vista previa con datos de ejemplo.
     *
     * @param code código del correo.
     * @return asunto y cuerpo renderizados.
     */
    @PostMapping("/{code}/preview")
    public RenderedEmail preview(@PathVariable EmailTemplateCode code) {
        return emailTemplateService.render(code, EmailPreviewData.model());
    }
}
