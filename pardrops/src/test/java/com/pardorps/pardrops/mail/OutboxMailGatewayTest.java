package com.pardorps.pardrops.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del outbox de correo.
 */
class OutboxMailGatewayTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Guarda el mensaje .eml y el cuerpo .html")
    void writesEmlAndHtml() throws Exception {
        OutboxMailGateway gateway = new OutboxMailGateway(
                tempDir.toString(), "no-reply@pardrops.test", "ParDrops");

        gateway.send(new OutgoingEmail(
                "vendedor@pardrops.com",
                "Nuevo pedido #12",
                "<p>Hola</p>"));

        List<Path> eml;
        List<Path> html;
        try (var files = Files.list(tempDir)) {
            List<Path> all = files.toList();
            eml = all.stream().filter(p -> p.toString().endsWith(".eml")).toList();
            html = all.stream().filter(p -> p.toString().endsWith(".html")).toList();
        }

        assertEquals(1, eml.size());
        assertEquals(1, html.size());
        assertTrue(Files.readString(html.get(0)).contains("<p>Hola</p>"));
        String raw = Files.readString(eml.get(0));
        assertTrue(raw.contains("vendedor@pardrops.com"));
        assertTrue(raw.contains("Nuevo pedido #12"));
    }

    @Test
    @DisplayName("Genera archivos distintos para correos distintos")
    void writesUniqueFiles() throws Exception {
        OutboxMailGateway gateway = new OutboxMailGateway(
                tempDir.toString(), "no-reply@pardrops.test", "ParDrops");

        gateway.send(new OutgoingEmail("a@pardrops.com", "Uno", "<p>1</p>"));
        gateway.send(new OutgoingEmail("a@pardrops.com", "Dos", "<p>2</p>"));

        try (var files = Files.list(tempDir)) {
            assertEquals(4, files.count());
        }
    }
}
