package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a las notificaciones de los usuarios.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Devuelve las notificaciones de un usuario, de la más reciente a la más
     * antigua.
     *
     * @param userId identificador del usuario.
     * @return listado de notificaciones.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Cuenta las notificaciones sin leer de un usuario.
     *
     * @param userId identificador del usuario.
     * @return número de avisos pendientes de leer.
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Marca como leídas todas las notificaciones pendientes de un usuario.
     *
     * @param userId identificador del usuario.
     * @return número de notificaciones actualizadas.
     */
    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllAsRead(@Param("userId") Long userId);
}
