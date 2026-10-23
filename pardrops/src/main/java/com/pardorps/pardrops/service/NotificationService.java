package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.NotificationResponse;
import com.pardorps.pardrops.dto.UnreadCountResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Notification;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio con la lógica de las notificaciones dentro de la aplicación.
 *
 * <p>Centraliza la creación de avisos y su consulta. Todas las lecturas y
 * actualizaciones se filtran por el usuario autenticado, de modo que un
 * usuario nunca puede leer ni marcar como leída la notificación de otro.</p>
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * @param notificationRepository acceso a las notificaciones.
     */
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Crea un aviso para un usuario.
     *
     * @param user    destinatario del aviso.
     * @param type    tipo del aviso.
     * @param title   título corto.
     * @param message texto explicativo.
     * @param link    ruta interna a la que lleva el aviso (opcional).
     */
    @Transactional
    public void notify(User user, NotificationType type, String title, String message, String link) {
        if (user == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    /**
     * Devuelve las notificaciones del usuario autenticado.
     *
     * @param user usuario autenticado.
     * @return listado de avisos, del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cuenta los avisos sin leer del usuario autenticado.
     *
     * @param user usuario autenticado.
     * @return número de avisos pendientes.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(User user) {
        return new UnreadCountResponse(
                notificationRepository.countByUserIdAndReadFalse(user.getId()));
    }

    /**
     * Marca como leída una notificación del usuario autenticado.
     *
     * @param user usuario autenticado.
     * @param id   identificador de la notificación.
     * @return la notificación actualizada.
     * @throws ApiException si la notificación no existe (404) o no es del
     *                     usuario autenticado (403).
     */
    @Transactional
    public NotificationResponse markAsRead(User user, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Notificación no encontrada: " + id));

        if (notification.getUser() == null
                || !notification.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Esa notificación no es tuya");
        }

        notification.setRead(true);
        return toResponse(notificationRepository.save(notification));
    }

    /**
     * Marca como leídas todas las notificaciones del usuario autenticado.
     *
     * @param user usuario autenticado.
     * @return número de avisos actualizados.
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsRead(user.getId());
    }

    /**
     * Convierte una entidad de notificación en su representación pública.
     *
     * @param notification notificación a convertir.
     * @return el aviso listo para enviar al cliente.
     */
    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
