package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AuthResponse;
import com.pardorps.pardrops.dto.GoogleExchangeRequest;
import com.pardorps.pardrops.dto.LoginRequest;
import com.pardorps.pardrops.dto.RegisterRequest;
import com.pardorps.pardrops.dto.UserResponse;
import com.pardorps.pardrops.security.GoogleExchangeRateLimiter;
import com.pardorps.pardrops.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de autenticación: registro, inicio de sesión y datos del usuario actual.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleExchangeRateLimiter googleExchangeRateLimiter;

    /**
     * @param authService              servicio de autenticación.
     * @param googleExchangeRateLimiter límite de intentos del canje de Google.
     */
    public AuthController(
            AuthService authService,
            GoogleExchangeRateLimiter googleExchangeRateLimiter
    ) {
        this.authService = authService;
        this.googleExchangeRateLimiter = googleExchangeRateLimiter;
    }

    /**
     * Registra una cuenta nueva.
     *
     * @param request datos del registro.
     * @return 201 con el token y el usuario creado.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Inicia sesión con correo y contraseña.
     *
     * @param request credenciales.
     * @return 200 con el token y el usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Canjea el código temporal que entrega el callback de Google.
     *
     * @param request código de un solo uso.
     * @return 200 con el token y el usuario.
     */
    @PostMapping("/google/exchange")
    public ResponseEntity<AuthResponse> exchangeGoogleCode(
            @Valid @RequestBody GoogleExchangeRequest request,
            HttpServletRequest httpRequest
    ) {
        googleExchangeRateLimiter.check(httpRequest.getRemoteAddr());
        return ResponseEntity.ok(authService.exchangeGoogleCode(request.code()));
    }

    /**
     * Devuelve los datos del usuario autenticado.
     *
     * @return 200 con el perfil del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.toResponse(authService.getCurrentUser()));
    }
}
