package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.NotificationResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Notification;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los avisos dentro de la aplicación ({@link NotificationService}).
 */
class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);

    private final NotificationService notificationService = new NotificationService(notificationRepository);

    @Test
    @DisplayName("Crea el aviso sin marcarlo como leído")
    void createsUnreadNotification() {
        User user = user(1L);

        notificationService.notify(user, NotificationType.ORDER, "Pedido recibido", "Recibimos tu pedido", "/pedidos");

        org.mockito.ArgumentCaptor<Notification> captor =
                org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertEquals(user, notification.getUser());
        assertEquals(NotificationType.ORDER, notification.getType());
        assertTrue(!notification.isRead());
    }

    @Test
    @DisplayName("Ignora el aviso si no hay destinatario")
    void ignoresNotificationWithoutRecipient() {
        notificationService.notify(null, NotificationType.ORDER, "Aviso", "Mensaje", null);

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Devuelve solo las notificaciones del usuario autenticado")
    void returnsNotificationsOfAuthenticatedUser() {
        User user = user(1L);
        Notification notification = notification(5L, user, "Pedido recibido");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> notifications = notificationService.findMine(user);

        assertEquals(1, notifications.size());
        assertEquals("Pedido recibido", notifications.get(0).title());
    }

    @Test
    @DisplayName("Impide marcar como leída la notificación de otro usuario")
    void rejectsMarkingAnotherUsersNotification() {
        User owner = user(1L);
        User intruder = user(2L);
        Notification notification = notification(5L, owner, "Pedido recibido");
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        ApiException exception = assertThrows(
                ApiException.class,
                () -> notificationService.markAsRead(intruder, 5L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Marca como leída la notificación del propio usuario")
    void marksOwnNotificationAsRead() {
        User owner = user(1L);
        Notification notification = notification(5L, owner, "Pedido recibido");
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(owner, 5L);

        assertTrue(response.read());
    }

    @Test
    @DisplayName("Devuelve 404 si la notificación no existe")
    void returnsNotFoundForMissingNotification() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> notificationService.markAsRead(user(1L), 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    @DisplayName("Cuenta los avisos sin leer del usuario")
    void countsUnreadNotifications() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(4L);

        assertEquals(4L, notificationService.countUnread(user(1L)).unreadCount());
    }

    /**
     * Construye un usuario de prueba.
     *
     * @param id identificador del usuario.
     * @return el usuario listo para usar.
     */
    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("usuario" + id + "@pardrops.com");
        user.setPassword("hash");
        user.setFullName("Usuario " + id);
        user.setRole(Role.CUSTOMER);
        return user;
    }

    /**
     * Construye una notificación de prueba.
     *
     * @param id      identificador de la notificación.
     * @param owner   destinatario del aviso.
     * @param title   título del aviso.
     * @return la notificación lista para usar.
     */
    private Notification notification(Long id, User owner, String title) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUser(owner);
        notification.setType(NotificationType.ORDER);
        notification.setTitle(title);
        notification.setMessage("Mensaje del aviso");
        notification.setRead(false);
        return notification;
    }
}
