package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.AuthResponse;
import com.pardorps.pardrops.dto.LoginRequest;
import com.pardorps.pardrops.dto.RegisterRequest;
import com.pardorps.pardrops.dto.UserResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import com.pardorps.pardrops.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Servicio que concentra la lógica de registro, inicio de sesión y consulta del
 * usuario autenticado.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * @param userRepository        acceso a los usuarios.
     * @param passwordEncoder       cifrado de contraseñas.
     * @param jwtService            generación de tokens.
     * @param authenticationManager validación de credenciales en el login.
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registra una cuenta nueva con rol de cliente y devuelve su token.
     *
     * @param request datos del registro.
     * @return el token y los datos públicos del usuario creado.
     * @throws ApiException si el correo ya está registrado.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Ya existe una cuenta con ese correo");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .createdAt(Instant.now())
                .build();

        User saved = userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(saved), toResponse(saved));
    }

    /**
     * Valida las credenciales y devuelve un token de acceso.
     *
     * @param request correo y contraseña.
     * @return el token y los datos públicos del usuario.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // Lanza BadCredentialsException si las credenciales no son correctas;
        // el manejador global la traduce a un 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Credenciales incorrectas"));

        return new AuthResponse(jwtService.generateToken(user), toResponse(user));
    }

    /**
     * Devuelve el usuario autenticado en la petición actual.
     *
     * @return la entidad del usuario.
     * @throws ApiException si no hay una sesión válida.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw ApiException.unauthorized("No hay una sesión iniciada");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.unauthorized("Usuario no encontrado"));
    }

    /**
     * Devuelve el usuario autenticado o {@code null} si la petición es anónima.
     *
     * <p>Se usa en la creación de pedidos, donde se admite tanto una compra
     * autenticada como una compra como invitado.</p>
     *
     * @return el usuario autenticado o nulo.
     */
    @Transactional(readOnly = true)
    public User getCurrentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    /**
     * Convierte una entidad de usuario a su representación pública.
     *
     * @param user usuario a convertir.
     * @return los datos públicos del usuario.
     */
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus()
        );
    }
}
