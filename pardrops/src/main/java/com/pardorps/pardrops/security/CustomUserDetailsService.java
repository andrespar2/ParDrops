package com.pardorps.pardrops.security;

import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adapta los usuarios de la base de datos al modelo de Spring Security.
 *
 * <p>Traduce cada {@link User} en un {@code UserDetails}, incluyendo el rol
 * como autoridad ({@code ROLE_ADMIN} o {@code ROLE_CUSTOMER}) para que las
 * reglas de autorización de la API funcionen.</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * @param userRepository repositorio de usuarios.
     */
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga un usuario por su correo electrónico.
     *
     * <p>Las cuentas suspendidas se cargan deshabilitadas: Spring Security
     * impide entonces tanto el inicio de sesión como la autenticación por
     * token, de modo que una cuenta suspendida pierde el acceso de inmediato.</p>
     *
     * @param email correo del usuario.
     * @return los datos del usuario entendidos por Spring Security.
     * @throws UsernameNotFoundException si no existe ninguna cuenta con ese correo.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isActive())
                .build();
    }
}
