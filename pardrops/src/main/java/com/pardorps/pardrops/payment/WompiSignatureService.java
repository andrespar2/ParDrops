package com.pardorps.pardrops.payment;

import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;

/**
 * Cálculo y comparación de las firmas de Wompi.
 *
 * <p>Implementa las dos firmas que exige la pasarela: la de integridad del
 * checkout (referencia + monto + moneda + secreto) y el checksum de los
 * eventos (valores de {@code signature.properties} + timestamp + secreto de
 * eventos).</p>
 */
@Component
public class WompiSignatureService {

    /**
     * Firma de integridad del checkout.
     *
     * @param reference       referencia única del pago.
     * @param amountInCents   monto en centavos.
     * @param currency        moneda (COP).
     * @param integritySecret secreto de integridad del ambiente.
     * @return el SHA-256 en hexadecimal minúscula.
     */
    public String integritySignature(
            String reference,
            long amountInCents,
            String currency,
            String integritySecret
    ) {
        return sha256(reference + amountInCents + currency + integritySecret);
    }

    /**
     * Checksum de un evento de Wompi.
     *
     * @param data          nodo {@code data} del evento.
     * @param properties    nombres de las propiedades en el orden indicado.
     * @param timestamp     timestamp UNIX del evento.
     * @param eventsSecret  secreto de eventos del ambiente.
     * @return el SHA-256 en hexadecimal minúscula.
     */
    public String eventChecksum(
            JsonNode data,
            Iterable<String> properties,
            long timestamp,
            String eventsSecret
    ) {
        StringBuilder value = new StringBuilder();
        Iterator<String> iterator = properties.iterator();
        while (iterator.hasNext()) {
            value.append(readPath(data, iterator.next()));
        }
        value.append(timestamp).append(eventsSecret);
        return sha256(value.toString());
    }

    /**
     * Compara dos firmas hexadecimales sin filtrar información por tiempos.
     *
     * @param expected firma calculada.
     * @param received firma recibida.
     * @return {@code true} si coinciden.
     */
    public boolean matches(String expected, String received) {
        if (expected == null || received == null) {
            return false;
        }
        byte[] a = expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Lee una propiedad con notación de puntos (por ejemplo
     * {@code transaction.status}) del nodo de datos.
     *
     * @param node       nodo raíz.
     * @param expression ruta de la propiedad.
     * @return el valor textual, o cadena vacía si no existe.
     */
    public String readPath(JsonNode node, String expression) {
        if (node == null || expression == null) {
            return "";
        }
        JsonNode current = node;
        for (String part : expression.split("\\.")) {
            if (current == null) {
                return "";
            }
            current = current.get(part);
        }
        if (current == null || current.isNull()) {
            return "";
        }
        return current.isTextual() ? current.asText() : current.asText();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible", exception);
        }
    }
}
