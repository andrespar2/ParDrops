package com.pardorps.pardrops.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración de la pasarela Wompi.
 *
 * <p>Las llaves privadas y los secretos solo se leen de variables de entorno.
 * La llave pública se entrega al navegador únicamente para abrir el widget de
 * checkout; el resto nunca sale del backend ni se registra en logs.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.wompi")
@Getter
@Setter
public class WompiProperties {

    /** Ambiente activo: {@code sandbox} o {@code production}. */
    private String env = "sandbox";

    /** Moneda de los cobros. Wompi Colombia solo admite COP. */
    private String currency = "COP";

    /** Cargo fijo de servicio por checkout, en pesos. */
    private double fixedFeeCop = 700.0;

    /** URL del frontend a la que Wompi redirige tras el pago. */
    private String frontendUrl = "http://localhost:4200";

    /** Ruta pública de la pantalla de resultado. */
    private String resultPath = "/compra";

    /** Credenciales del ambiente de pruebas. */
    private Keys test = new Keys();

    /** Credenciales del ambiente de producción. */
    private Keys prod = new Keys();

    /**
     * Juego de credenciales de un ambiente.
     */
    @Getter
    @Setter
    public static class Keys {

        /** Llave pública (puede enviarse al frontend). */
        private String publicKey = "";

        /** Llave privada (solo backend). */
        private String privateKey = "";

        /** Secreto para validar los eventos (solo backend). */
        private String eventsSecret = "";

        /** Secreto para firmar la integridad (solo backend). */
        private String integritySecret = "";
    }

    /**
     * Indica si el ambiente activo es Sandbox.
     *
     * @return {@code true} si {@code env} es {@code sandbox} o {@code test}.
     */
    public boolean isSandbox() {
        return env == null || env.isBlank()
                || "sandbox".equalsIgnoreCase(env)
                || "test".equalsIgnoreCase(env);
    }

    /**
     * Nombre del ambiente tal como lo informa Wompi en los eventos.
     *
     * @return {@code test} en Sandbox y {@code prod} en Producción.
     */
    public String activeEnvironment() {
        return isSandbox() ? "test" : "prod";
    }

    /**
     * Credenciales del ambiente activo.
     *
     * @return el juego de llaves correspondiente.
     */
    public Keys activeKeys() {
        return isSandbox() ? test : prod;
    }

    /**
     * Llave pública del ambiente activo.
     *
     * @return la llave pública.
     */
    public String publicKey() {
        return trim(activeKeys().getPublicKey());
    }

    /**
     * Llave privada del ambiente activo.
     *
     * @return la llave privada.
     */
    public String privateKey() {
        return trim(activeKeys().getPrivateKey());
    }

    /**
     * Secreto de eventos del ambiente activo.
     *
     * @return el secreto.
     */
    public String eventsSecret() {
        return trim(activeKeys().getEventsSecret());
    }

    /**
     * Secreto de integridad del ambiente activo.
     *
     * @return el secreto.
     */
    public String integritySecret() {
        return trim(activeKeys().getIntegritySecret());
    }

    /**
     * URL base de la API de Wompi para el ambiente activo.
     *
     * @return la URL sin barra final.
     */
    public String apiBaseUrl() {
        return isSandbox()
                ? "https://sandbox.wompi.co/v1"
                : "https://production.wompi.co/v1";
    }

    /**
     * Indica si hay llaves suficientes para iniciar un checkout.
     *
     * @return {@code true} si están la llave pública y el secreto de integridad.
     */
    public boolean isCheckoutConfigured() {
        return !publicKey().isEmpty() && !integritySecret().isEmpty();
    }

    /**
     * Indica si hay llaves para verificar transacciones contra la API.
     *
     * @return {@code true} si está la llave privada.
     */
    public boolean isVerificationConfigured() {
        return !privateKey().isEmpty();
    }

    /**
     * Construye la URL pública de la pantalla de resultado.
     *
     * @param reference referencia del pedido.
     * @return la URL completa.
     */
    public String resultUrl(String reference) {
        String base = frontendUrl == null ? "" : frontendUrl.replaceAll("/+$", "");
        String path = resultPath == null || resultPath.isBlank() ? "/compra" : resultPath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path + "/" + reference;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
