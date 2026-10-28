package com.pardorps.pardrops.mail;

import com.pardorps.pardrops.dto.RenderedEmail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del renderizador de plantillas.
 */
class EmailTemplateRendererTest {

    @Test
    @DisplayName("Sustituye las variables del asunto y del cuerpo")
    void replacesVariables() {
        RenderedEmail rendered = EmailTemplateRenderer.render(
                "Pedido #{{orderId}}",
                "<p>Hola {{customerName}}</p>",
                Map.of("orderId", "12", "customerName", "Ana"));

        assertEquals("Pedido #12", rendered.subject());
        assertTrue(rendered.bodyHtml().contains("Hola Ana"));
    }

    @Test
    @DisplayName("Escapa el HTML de los valores en el cuerpo")
    void escapesUserValues() {
        RenderedEmail rendered = EmailTemplateRenderer.render(
                "Asunto",
                "<p>{{customerName}}</p>",
                Map.of("customerName", "<img src=x onerror=alert(1)>"));

        assertTrue(rendered.bodyHtml().contains("&lt;img"));
        assertTrue(!rendered.bodyHtml().contains("<img"));
    }

    @Test
    @DisplayName("El asunto no se escapa como HTML")
    void subjectIsNotEscaped() {
        RenderedEmail rendered = EmailTemplateRenderer.render(
                "Pedido #{{orderId}}", "cuerpo", Map.of("orderId", "12 & 13"));

        assertEquals("Pedido #12 & 13", rendered.subject());
    }

    @Test
    @DisplayName("Inserta sin escapar los fragmentos marcados como seguros")
    void insertsSafeHtml() {
        RenderedEmail rendered = EmailTemplateRenderer.render(
                "Asunto",
                "{{itemsTable}}",
                Map.of("itemsTable",
                        new EmailTemplateRenderer.SafeHtml("<table><tr><td>Producto</td></tr></table>")));

        assertTrue(rendered.bodyHtml().contains("<table>"));
    }

    @Test
    @DisplayName("Una variable desconocida se sustituye por vacío")
    void unknownVariableBecomesEmpty() {
        RenderedEmail rendered = EmailTemplateRenderer.render(
                "Asunto", "<p>{{noExiste}}</p>", Map.of());

        assertTrue(rendered.bodyHtml().contains("<p></p>"));
    }
}
