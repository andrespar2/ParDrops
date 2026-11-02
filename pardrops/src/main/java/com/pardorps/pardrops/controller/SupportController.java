package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.CreateConversationRequest;
import com.pardorps.pardrops.dto.SendSupportMessageRequest;
import com.pardorps.pardrops.dto.SupportConversationResponse;
import com.pardorps.pardrops.dto.SupportMessageResponse;
import com.pardorps.pardrops.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de soporte para el usuario autenticado.
 *
 * <p>Cada usuario solo puede abrir y consultar sus propias conversaciones.</p>
 */
@RestController
@RequestMapping("/api/support/conversations")
public class SupportController {

    private final SupportService supportService;

    /**
     * @param supportService servicio de conversaciones.
     */
    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    /** Lista las conversaciones del usuario. */
    @GetMapping
    public List<SupportConversationResponse> list() {
        return supportService.listMine();
    }

    /** Abre una conversación nueva. */
    @PostMapping
    public ResponseEntity<SupportConversationResponse> create(
            @Valid @RequestBody CreateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.create(request));
    }

    /** Mensajes de una conversación propia. */
    @GetMapping("/{id}/messages")
    public List<SupportMessageResponse> messages(@PathVariable Long id) {
        return supportService.messages(id);
    }

    /** Envía un mensaje en una conversación propia. */
    @PostMapping("/{id}/messages")
    public ResponseEntity<SupportMessageResponse> send(
            @PathVariable Long id,
            @Valid @RequestBody SendSupportMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportService.send(id, request));
    }
}
