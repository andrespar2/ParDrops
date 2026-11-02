package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SupportConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a las conversaciones de soporte. */
public interface SupportConversationRepository extends JpaRepository<SupportConversation, Long> {

    /**
     * Conversaciones de un usuario, de la más reciente a la más antigua.
     *
     * @param userId identificador del usuario.
     * @return conversaciones del usuario.
     */
    List<SupportConversation> findByUserIdOrderByLastMessageAtDesc(Long userId);

    /**
     * Todas las conversaciones para el panel de soporte.
     *
     * @return conversaciones ordenadas por actividad.
     */
    List<SupportConversation> findAllByOrderByLastMessageAtDesc();

    /**
     * Cuenta las conversaciones pendientes.
     *
     * @param status estado a contar.
     * @return número de conversaciones.
     */
    long countByStatus(com.pardorps.pardrops.model.SupportStatus status);

    /**
     * Cuenta las conversaciones con mensajes sin leer para soporte.
     *
     * @return número de conversaciones pendientes de lectura.
     */
    long countByStaffUnreadTrue();
}
