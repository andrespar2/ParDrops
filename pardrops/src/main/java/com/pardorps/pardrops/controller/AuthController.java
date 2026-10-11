package com.pardorps.pardrops.controller;

import com.pardorps.pardrops.dto.AuthResponse;
import com.pardorps.pardrops.dto.LoginRequest;
import com.pardorps.pardrops.dto.RegisterRequest;
import com.pardorps.pardrops.dto.UserResponse;
import com.pardorps.pardrops.service.AuthService;
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

    /**
     * @param authService servicio de autenticación.
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
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
     * Devuelve los datos del usuario autenticado.
     *
     * @return 200 con el perfil del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.toResponse(authService.getCurrentUser()));
    }
}
