package com.pardorps.pardrops.storage;

import com.pardorps.pardrops.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Almacén de objetos compatible con Cloudflare R2 (API S3).
 *
 * <p>Se activa con {@code app.media.storage=r2}. Las credenciales S3 de R2
 * son distintas del API Token de Cloudflare y llegan solo por entorno. Si no
 * están configuradas, la aplicación no selecciona este almacén y sigue usando
 * el almacenamiento actual.</p>
 *
 * <p>Las imágenes públicas se sirven desde el dominio CDN configurado en
 * {@code app.media.r2.public-base-url}. Los objetos se guardan con un nombre
 * UUID versionado y {@code Cache-Control} de un año inmutable.</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.media.storage", havingValue = "r2")
public class R2MediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(R2MediaStorage.class);
    private static final String SERVICE = "s3";
    private static final String REGION = "auto";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final DateTimeFormatter AMZ_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String publicBaseUrl;
    private final HttpClient httpClient;

    public R2MediaStorage(
            @Value("${app.media.r2.endpoint:}") String endpoint,
            @Value("${app.media.r2.bucket:}") String bucket,
            @Value("${app.media.r2.access-key:}") String accessKey,
            @Value("${app.media.r2.secret-key:}") String secretKey,
            @Value("${app.media.r2.public-base-url:}") String publicBaseUrl
    ) {
        this.endpoint = trimSlash(endpoint);
        this.bucket = bucket == null ? "" : bucket.trim();
        this.accessKey = accessKey == null ? "" : accessKey.trim();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.publicBaseUrl = trimSlash(publicBaseUrl);
        if (this.endpoint.isBlank() || this.bucket.isBlank()
                || this.accessKey.isBlank() || this.secretKey.isBlank()
                || this.publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "El almacenamiento R2 requiere endpoint, bucket, access-key, secret-key "
                            + "y public-base-url configurados por variables de entorno");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        log.info("Almacenamiento de imágenes activo: Cloudflare R2 (bucket {})", this.bucket);
    }

    @Override
    public StoredFile store(String storedName, byte[] content) {
        String mimeType = ImageTypeDetector.detect(content);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        String key = bucket + "/" + storedName;
        HttpRequest.Builder builder = signedRequest("PUT", key, content)
                .header("Content-Type", mimeType)
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        send(builder, "guardar la imagen en R2");
        return new StoredFile(storedName, publicPath(storedName), content.length);
    }

    @Override
    public boolean delete(String storedName) {
        String key = bucket + "/" + storedName;
        HttpRequest.Builder builder = signedRequest("DELETE", key, new byte[0])
                .DELETE();
        try {
            HttpResponse<Void> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception exception) {
            log.warn("No se pudo borrar la imagen de R2: {}", exception.getMessage());
            return false;
        }
    }

    @Override
    public String publicPath(String storedName) {
        return publicBaseUrl + "/" + storedName;
    }

    @Override
    public String basePublicPath() {
        return publicBaseUrl;
    }

    @Override
    public String storageLocation() {
        return endpoint + "/" + bucket;
    }

    private HttpRequest.Builder signedRequest(String method, String key, byte[] payload) {
        String amzDate = AMZ_DATE.format(Instant.now());
        String dateStamp = DATE_STAMP.format(Instant.now());
        String payloadHash = sha256Hex(payload);
        String host = URI.create(endpoint).getHost();
        String canonicalUri = "/" + encodePath(key);

        String canonicalHeaders = "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = method + "\n" + canonicalUri + "\n\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;

        String scope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + scope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = signingKey(dateStamp);
        String signature = hex(hmac(signingKey, stringToSign));
        String authorization = ALGORITHM + " Credential=" + accessKey + "/" + scope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

        return HttpRequest.newBuilder()
                .uri(URI.create(endpoint + canonicalUri))
                .timeout(Duration.ofSeconds(20))
                .header("Host", host)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Authorization", authorization);
    }

    private void send(HttpRequest.Builder builder, String action) {
        try {
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("R2 rechazó {}: HTTP {} {}", action, response.statusCode(), response.body());
                throw ApiException.badRequest("No se pudo " + action);
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.badRequest("No se pudo " + action + ": " + exception.getMessage());
        }
    }

    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] regionKey = hmac(dateKey, REGION);
        byte[] serviceKey = hmac(regionKey, SERVICE);
        return hmac(serviceKey, "aws4_request");
    }

    private byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar la petición a R2", exception);
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular SHA-256", exception);
        }
    }

    private String hex(byte[] data) {
        return HexFormat.of().formatHex(data).toLowerCase(Locale.ROOT);
    }

    private String encodePath(String value) {
        StringBuilder encoded = new StringBuilder();
        for (String segment : value.split("/")) {
            if (encoded.length() > 0) {
                encoded.append('/');
            }
            encoded.append(java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8)
                    .replace("+", "%20"));
        }
        return encoded.toString();
    }

    private String trimSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
