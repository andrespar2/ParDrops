package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.service.EmailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Herramienta de administración para comprobar la entrega de correo.
 *
 * <p>Solo accesible para administradores; devuelve si el servidor SMTP aceptó
 * el mensaje, sin exponer credenciales.</p>
 */
@RestController
@RequestMapping("/api/admin/mail")
public class AdminMailController {

    private final EmailService emailService;

    /**
     * @param emailService servicio de correo.
     */
    public AdminMailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Envía un correo de prueba.
     *
     * @param request destinatario.
     * @return resultado del envío.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@RequestBody TestMailRequest request) {
        String html = "<div style=\"font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;padding:24px\">"
                + "<h1 style=\"color:#111827\">ParDrops</h1>"
                + "<p>Este es un correo de prueba enviado desde la API de ParDrops con Brevo SMTP.</p>"
                + "<p style=\"color:#6b7280;font-size:13px\">Si recibes este mensaje, el envío de correo está operativo.</p>"
                + "</div>";
        boolean sent = emailService.send(request.to(), "ParDrops · Prueba de correo", html);
        return ResponseEntity.ok(Map.of("sent", sent, "to", request.to()));
    }

    /** Datos del correo de prueba. */
    public record TestMailRequest(
            @NotBlank(message = "El destinatario es obligatorio")
            @Email(message = "El correo no es válido")
            String to
    ) {
    }
}
