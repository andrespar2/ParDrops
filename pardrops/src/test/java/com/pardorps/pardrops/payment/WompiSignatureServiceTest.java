package com.pardorps.pardrops.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las firmas de Wompi con los vectores de la documentación oficial.
 */
class WompiSignatureServiceTest {

    private final WompiSignatureService service = new WompiSignatureService();

    @Test
    @DisplayName("Calcula la firma de integridad del checkout")
    void calculatesIntegritySignature() {
        String signature = service.integritySignature(
                "sk8-438k4-xmxm392-sn2m",
                2_490_000L,
                "COP",
                "prod_integrity_Z5mMke9x0k8gpErbDqwrJXMqsI6SFli6");

        assertEquals(
                "37c8407747e595535433ef8f6a811d853cd943046624a0ec04662b17bbf33bf5",
                signature);
    }

    @Test
    @DisplayName("Calcula el checksum de un evento en el orden de properties")
    void calculatesEventChecksum() {
        String body = """
                {
                  "data": {
                    "transaction": {
                      "id": "1234-1610641025-49201",
                      "status": "APPROVED",
                      "amount_in_cents": 4490000
                    }
                  }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        var data = mapper.readTree(body).path("data");
        String checksum = service.eventChecksum(
                data,
                List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"),
                1_530_291_411L,
                "prod_events_OcHnIzeBl5socpwByQ4hA52Em3USQ93Z");

        assertEquals(
                "5A18EC5E8FDB7DF463E9F94774CBA8F583BA21BD04A09CEFF2EA68A4BC0AEFBE",
                checksum.toUpperCase());
    }

    @Test
    @DisplayName("Compara firmas sin distinguir mayúsculas y rechaza valores distintos")
    void comparesSignaturesSafely() {
        assertTrue(service.matches("abc123", "ABC123"));
        assertFalse(service.matches("abc123", "abc124"));
        assertFalse(service.matches(null, "abc"));
    }

    @Test
    @DisplayName("Lee propiedades anidadas y devuelve vacío si no existen")
    void readsNestedProperties() {
        ObjectMapper mapper = new ObjectMapper();
        var data = mapper.readTree("""
                {"transaction": {"id": "tx-1", "status": "APPROVED"}}
                """);

        assertEquals("tx-1", service.readPath(data, "transaction.id"));
        assertEquals("APPROVED", service.readPath(data, "transaction.status"));
        assertEquals("", service.readPath(data, "transaction.missing"));
        assertEquals("", service.readPath(data, "unknown.path"));
    }
}
