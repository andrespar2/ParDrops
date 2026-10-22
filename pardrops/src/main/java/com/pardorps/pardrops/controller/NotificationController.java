package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.NotificationResponse;
import com.pardorps.pardrops.dto.UnreadCountResponse;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.service.AuthService;
import com.pardorps.pardrops.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de las notificaciones dentro de la aplicación.
 *
 * <p>Todas las operaciones trabajamos sobre las notificaciones del usuario
 * autenticado: nunca se acepta un identificador de usuario en la petición, de
 * modo que un usuario no puede leer los avisos de otro.</p>
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    /**
     * @param notificationService servicio de notificaciones.
     * @param authService          servicio para obtener el usuario autenticado.
     */
    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    /**
     * Lista las notificaciones del usuario autenticado.
     *
     * @return 200 con el listado de avisos.
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.findMine(user));
    }

    /**
     * Cuenta los avisos pendientes de leer.
     *
     * @return 200 con el número de avisos sin leer.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.countUnread(user));
    }

    /**
     * Marca una notificación como leída.
     *
     * @param id identificador de la notificación.
     * @return 200 con la notificación actualizada.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(notificationService.markAsRead(user, id));
    }

    /**
     * Marca como leídas todas las notificaciones del usuario.
     *
     * @return 200 con el número de avisos actualizados.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<UnreadCountResponse> markAllAsRead() {
        User user = authService.getCurrentUser();
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(notificationService.countUnread(user));
    }
}
