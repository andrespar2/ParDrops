package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateConversationRequest;
import com.pardorps.pardrops.dto.SendSupportMessageRequest;
import com.pardorps.pardrops.dto.SupportConversationResponse;
import com.pardorps.pardrops.dto.SupportMessageResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.SupportConversation;
import com.pardorps.pardrops.model.SupportMessage;
import com.pardorps.pardrops.model.SupportStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.SupportConversationRepository;
import com.pardorps.pardrops.repository.SupportMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Conversaciones de soporte entre usuarios y administración.
 *
 * <p>Un usuario solo accede a sus propias conversaciones; el personal
 * administrativo accede a todas. Los mensajes se guardan en PostgreSQL y se
 * consultan bajo demanda para no consumir recursos de más.</p>
 */
@Service
public class SupportService {

    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final AuthService authService;

    /**
     * @param conversationRepository acceso a las conversaciones.
     * @param messageRepository      acceso a los mensajes.
     * @param authService            usuario autenticado.
     */
    public SupportService(
            SupportConversationRepository conversationRepository,
            SupportMessageRepository messageRepository,
            AuthService authService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.authService = authService;
    }

    /**
     * Conversaciones del usuario autenticado.
     *
     * @return conversaciones con su último mensaje.
     */
    @Transactional(readOnly = true)
    public List<SupportConversationResponse> listMine() {
        User user = authService.getCurrentUser();
        return conversationRepository.findByUserIdOrderByLastMessageAtDesc(user.getId()).stream()
                .map(conversation -> toResponse(conversation, conversation.isUserUnread()))
                .toList();
    }

    /**
     * Abre una conversación de soporte.
     *
     * @param request asunto y primer mensaje.
     * @return la conversación creada.
     */
    @Transactional
    public SupportConversationResponse create(CreateConversationRequest request) {
        User user = authService.getCurrentUser();

        SupportConversation conversation = new SupportConversation();
        conversation.setUser(user);
        conversation.setSubject(request.subject().trim());
        conversation.setStatus(SupportStatus.OPEN);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());
        conversation.setUserUnread(false);
        conversation.setStaffUnread(true);
        ConversationHolder holder = new ConversationHolder(conversationRepository.save(conversation));

        SupportMessage message = message(holder.conversation, user, false, request.message());
        messageRepository.save(message);

        return toResponse(holder.conversation, false);
    }

    /**
     * Mensajes de una conversación propia.
     *
     * @param id identificador de la conversación.
     * @return mensajes en orden cronológico.
     */
    @Transactional
    public List<SupportMessageResponse> messages(Long id) {
        User user = authService.getCurrentUser();
        SupportConversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La conversación no existe"));

        if (isStaff(user)) {
            conversation.setStaffUnread(false);
        } else {
            if (!conversation.getUser().getId().equals(user.getId())) {
                throw ApiException.forbidden("Esa conversación no es tuya");
            }
            conversation.setUserUnread(false);
        }
        conversationRepository.save(conversation);

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(id).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Envía un mensaje del usuario a su conversación.
     *
     * @param id      identificador de la conversación.
     * @param request contenido.
     * @return el mensaje guardado.
     */
    @Transactional
    public SupportMessageResponse send(Long id, SendSupportMessageRequest request) {
        User user = authService.getCurrentUser();
        SupportConversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La conversación no existe"));

        if (!conversation.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("Esa conversación no es tuya");
        }

        SupportMessage message = message(conversation, user, false, request.body());
        SupportMessage saved = messageRepository.save(message);
        conversation.setStatus(SupportStatus.OPEN);
        conversation.setStaffUnread(true);
        conversation.setUserUnread(false);
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);
        return toResponse(saved);
    }

    /**
     * Conversaciones para el panel de soporte.
     *
     * @return todas las conversaciones ordenadas por actividad.
     */
    @Transactional(readOnly = true)
    public List<SupportConversationResponse> listForStaff() {
        requireStaff();
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .map(conversation -> toResponse(conversation, conversation.isStaffUnread()))
                .toList();
    }

    /**
     * Responde una conversación como soporte.
     *
     * @param id      identificador de la conversación.
     * @param request contenido.
     * @return el mensaje guardado.
     */
    @Transactional
    public SupportMessageResponse reply(Long id, SendSupportMessageRequest request) {
        User staff = requireStaff();
        SupportConversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La conversación no existe"));

        SupportMessage message = message(conversation, staff, true, request.body());
        SupportMessage saved = messageRepository.save(message);
        conversation.setUserUnread(true);
        conversation.setStaffUnread(false);
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);
        return toResponse(saved);
    }

    /**
     * Marca una conversación como resuelta.
     *
     * @param id identificador de la conversación.
     */
    @Transactional
    public void resolve(Long id) {
        requireStaff();
        SupportConversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("La conversación no existe"));
        conversation.setStatus(SupportStatus.RESOLVED);
        conversationRepository.save(conversation);
    }

    /** Indica si el usuario es personal administrativo. */
    private boolean isStaff(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** Exige personal administrativo. */
    private User requireStaff() {
        User user = authService.getCurrentUser();
        if (!isStaff(user)) {
            throw ApiException.forbidden("Solo el equipo de soporte puede responder");
        }
        return user;
    }

    /** Crea un mensaje asociado a la conversación. */
    private SupportMessage message(SupportConversation conversation, User author, boolean staff, String body) {
        SupportMessage message = new SupportMessage();
        message.setConversation(conversation);
        message.setAuthor(author);
        message.setAuthorName(author.getFullName());
        message.setFromStaff(staff);
        message.setBody(body.trim());
        message.setCreatedAt(Instant.now());
        return message;
    }

    /** Convierte la conversación en su resumen. */
    private SupportConversationResponse toResponse(SupportConversation conversation, boolean unread) {
        String last = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(SupportMessage::getBody)
                .orElse("");
        return new SupportConversationResponse(
                conversation.getId(),
                conversation.getSubject(),
                conversation.getStatus(),
                last,
                conversation.getLastMessageAt(),
                unread,
                conversation.getCreatedAt()
        );
    }

    /** Convierte el mensaje en su representación pública. */
    private SupportMessageResponse toResponse(SupportMessage message) {
        return new SupportMessageResponse(
                message.getId(),
                message.getBody(),
                message.isFromStaff(),
                message.getAuthorName(),
                message.getCreatedAt()
        );
    }

    /** Envoltorio para mantener la conversación guardada disponible. */
    private record ConversationHolder(SupportConversation conversation) {
    }
}
