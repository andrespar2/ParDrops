package com.pardorps.pardrops.security;

import com.pardorps.pardrops.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Servicio encargado de crear y validar los tokens JWT.
 *
 * <p>El token incluye como "subject" el correo del usuario y una claim
 * {@code role} con su rol, de forma que el servidor pueda autorizar cada
 * petición sin necesidad de consultar la base de datos en cada acceso.</p>
 */
@Service
public class JwtService {

    /** Clave secreta usada para firmar y verificar los tokens. */
    private final SecretKey signingKey;

    /** Tiempo de validez de cada token, en milisegundos. */
    private final long expirationMs;

    /**
     * Crea el servicio a partir de la configuración de la aplicación.
     *
     * @param secret       clave secreta (mínimo 32 caracteres para HS256).
     * @param expirationMs duración del token en milisegundos.
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT firmado para el usuario indicado.
     *
     * @param user usuario autenticado.
     * @return token JWT compacto listo para enviar al cliente.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el correo del usuario almacenado en el token.
     *
     * @param token token JWT.
     * @return correo del usuario (subject).
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Comprueba si el token es válido para el correo indicado y no ha expirado.
     *
     * @param token token JWT.
     * @param email correo esperado.
     * @return {@code true} si el token es válido.
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String subject = extractEmail(token);
            return subject.equals(email) && !isExpired(token);
        } catch (Exception exception) {
            // Firma inválida, token malformado, caducado, etc.
            return false;
        }
    }

    /**
     * Indica si el token ya ha expirado.
     *
     * @param token token JWT.
     * @return {@code true} si la fecha de expiración es anterior a "ahora".
     */
    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Extrae una claim concreta del token usando la función indicada.
     *
     * @param token          token JWT.
     * @param claimsResolver función que transforma las claims en el valor buscado.
     * @param <T>            tipo del valor devuelto.
     * @return el valor de la claim.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
