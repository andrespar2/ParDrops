package com.pardorps.pardrops.repository;

import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del repositorio de usuarios centradas en la identidad de Google.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Encuentra la cuenta por proveedor e identificador externo")
    void findsAccountByProviderAndProviderId() {
        User google = User.builder()
                .email("google@pardrops.com")
                .password("hash")
                .fullName("Cuenta Google")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("sub-123")
                .build();
        userRepository.save(google);

        Optional<User> found = userRepository.findByAuthProviderAndProviderId(
                AuthProvider.GOOGLE, "sub-123");

        assertTrue(found.isPresent());
        assertEquals("google@pardrops.com", found.get().getEmail());
    }

    @Test
    @DisplayName("No confunde cuentas locales con identificadores de Google")
    void doesNotMatchLocalAccount() {
        userRepository.save(User.builder()
                .email("local@pardrops.com")
                .password("hash")
                .fullName("Cuenta local")
                .role(Role.CUSTOMER)
                .build());

        Optional<User> found = userRepository.findByAuthProviderAndProviderId(
                AuthProvider.GOOGLE, "sub-123");

        assertTrue(found.isEmpty());
    }
}
