package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.SendSupportMessageRequest;
import com.pardorps.pardrops.dto.SupportConversationResponse;
import com.pardorps.pardrops.dto.SupportMessageResponse;
import com.pardorps.pardrops.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de soporte para administración.
 *
 * <p>El acceso está restringido al rol ADMIN por la configuración de
 * seguridad; el servicio comprueba además que el usuario sea personal.</p>
 */
@RestController
@RequestMapping("/api/admin/support/conversations")
public class AdminSupportController {

    private final SupportService supportService;

    /**
     * @param supportService servicio de conversaciones.
     */
    public AdminSupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    /** Lista todas las conversaciones. */
    @GetMapping
    public List<SupportConversationResponse> list() {
        return supportService.listForStaff();
    }

    /** Mensajes de una conversación. */
    @GetMapping("/{id}/messages")
    public List<SupportMessageResponse> messages(@PathVariable Long id) {
        return supportService.messages(id);
    }

    /** Responde a una conversación. */
    @PostMapping("/{id}/messages")
    public ResponseEntity<SupportMessageResponse> reply(
            @PathVariable Long id,
            @Valid @RequestBody SendSupportMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.reply(id, request));
    }

    /** Marca la conversación como resuelta. */
    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long id) {
        supportService.resolve(id);
        return ResponseEntity.noContent().build();
    }
}
