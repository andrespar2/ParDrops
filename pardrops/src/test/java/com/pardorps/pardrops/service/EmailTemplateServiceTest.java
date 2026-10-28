package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.EmailTemplateRequest;
import com.pardorps.pardrops.dto.EmailTemplateResponse;
import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.mail.EmailTemplateDefaults;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.EmailTemplate;
import com.pardorps.pardrops.model.EmailTemplateCode;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.EmailTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de las plantillas de correo y su renderizado.
 */
class EmailTemplateServiceTest {

    private final EmailTemplateRepository repository = mock(EmailTemplateRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final EmailTemplateService service = new EmailTemplateService(repository, auditLogService);

    @Test
    @DisplayName("Crea las tres plantillas cuando no existen")
    void seedsMissingTemplates() {
        when(repository.existsByCode(any())).thenReturn(false);

        service.seedDefaults();

        verify(repository, org.mockito.Mockito.times(3)).save(any(EmailTemplate.class));
    }

    @Test
    @DisplayName("No sobrescribe una plantilla que ya existe")
    void doesNotOverwriteExistingTemplate() {
        when(repository.existsByCode(any())).thenReturn(true);

        service.seedDefaults();

        verify(repository, never()).save(any(EmailTemplate.class));
    }

    @Test
    @DisplayName("Actualiza el asunto, el cuerpo y registra la auditoría")
    void updatesTemplate() {
        EmailTemplate template = template(EmailTemplateCode.SELLER_NEW_ORDER);
        when(repository.findByCode(EmailTemplateCode.SELLER_NEW_ORDER)).thenReturn(Optional.of(template));
        when(repository.save(template)).thenReturn(template);
        User admin = User.builder().id(1L).fullName("Admin").build();

        EmailTemplateResponse response = service.update(
                admin,
                EmailTemplateCode.SELLER_NEW_ORDER,
                new EmailTemplateRequest("Nuevo asunto", "<p>Hola {{sellerName}}</p>", false));

        assertEquals("Nuevo asunto", response.subject());
        assertFalse(response.active());
        verify(repository).save(template);
        verify(auditLogService).record(
                eq(admin), eq(AuditAction.EMAIL_TEMPLATE_UPDATED),
                eq("EMAIL_TEMPLATE"), eq("SELLER_NEW_ORDER"), any());
    }

    @Test
    @DisplayName("Restaura la plantilla predeterminada")
    void resetsTemplate() {
        EmailTemplate template = template(EmailTemplateCode.BUYER_ORDER_STATUS);
        template.setSubject("Personalizado");
        when(repository.findByCode(EmailTemplateCode.BUYER_ORDER_STATUS)).thenReturn(Optional.of(template));
        when(repository.save(template)).thenReturn(template);

        EmailTemplateResponse response = service.reset(null, EmailTemplateCode.BUYER_ORDER_STATUS);

        EmailTemplateDefaults.DefaultTemplate defaults =
                EmailTemplateDefaults.of(EmailTemplateCode.BUYER_ORDER_STATUS);
        assertEquals(defaults.subject(), response.subject());
        assertEquals(defaults.bodyHtml(), template.getBodyHtml());
        verify(auditLogService).record(
                eq(null), eq(AuditAction.EMAIL_TEMPLATE_RESET),
                eq("EMAIL_TEMPLATE"), eq("BUYER_ORDER_STATUS"), any());
    }

    @Test
    @DisplayName("Renderiza las variables y escapa el HTML del usuario")
    void rendersAndEscapesValues() {
        EmailTemplate template = template(EmailTemplateCode.BUYER_ORDER_STATUS);
        template.setSubject("Pedido #{{orderId}}");
        template.setBodyHtml("<p>Hola {{customerName}}</p>{{itemsTable}}");
        when(repository.findByCode(EmailTemplateCode.BUYER_ORDER_STATUS)).thenReturn(Optional.of(template));

        RenderedEmail rendered = service.renderIfActive(
                EmailTemplateCode.BUYER_ORDER_STATUS,
                Map.of("orderId", "12", "customerName", "<script>alert(1)</script>",
                        "itemsTable", new com.pardorps.pardrops.mail.EmailTemplateRenderer.SafeHtml("<table></table>")))
                .orElseThrow();

        assertEquals("Pedido #12", rendered.subject());
        assertTrue(rendered.bodyHtml().contains("&lt;script&gt;"));
        assertFalse(rendered.bodyHtml().contains("<script>"));
        assertTrue(rendered.bodyHtml().contains("<table></table>"));
    }

    @Test
    @DisplayName("No renderiza una plantilla inactiva")
    void skipsInactiveTemplate() {
        EmailTemplate template = template(EmailTemplateCode.BUYER_ORDER_STATUS);
        template.setActive(false);
        when(repository.findByCode(EmailTemplateCode.BUYER_ORDER_STATUS)).thenReturn(Optional.of(template));

        assertTrue(service.renderIfActive(EmailTemplateCode.BUYER_ORDER_STATUS, Map.of()).isEmpty());
    }

    @Test
    @DisplayName("Devuelve 404 si la plantilla no existe")
    void unknownTemplateReturnsNotFound() {
        when(repository.findByCode(EmailTemplateCode.BUYER_PAYMENT_RECEIPT)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.findByCode(EmailTemplateCode.BUYER_PAYMENT_RECEIPT));

        assertEquals(404, error.getStatus().value());
    }

    private EmailTemplate template(EmailTemplateCode code) {
        EmailTemplate template = new EmailTemplate();
        template.setId(1L);
        template.setCode(code);
        template.setName("Plantilla");
        template.setSubject("Asunto");
        template.setBodyHtml("<p>Contenido</p>");
        template.setActive(true);
        template.setUpdatedAt(Instant.now());
        return template;
    }
}
