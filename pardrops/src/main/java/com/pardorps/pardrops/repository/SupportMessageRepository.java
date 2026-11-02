package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a los mensajes de soporte. */
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    /**
     * Mensajes de una conversación en orden cronológico.
     *
     * @param conversationId identificador de la conversación.
     * @return mensajes de la conversación.
     */
    List<SupportMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Último mensaje de una conversación.
     *
     * @param conversationId identificador de la conversación.
     * @return el mensaje más reciente, si existe.
     */
    java.util.Optional<SupportMessage> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
