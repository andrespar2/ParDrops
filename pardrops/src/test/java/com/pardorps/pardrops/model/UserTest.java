package com.pardorps.pardrops.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del modelo de cuenta con el origen de autenticación.
 */
class UserTest {

    @Test
    @DisplayName("Una cuenta nueva es local por defecto")
    void newAccountIsLocalByDefault() {
        User user = User.builder()
                .email("cliente@pardrops.com")
                .password("hash")
                .fullName("Cliente")
                .role(Role.CUSTOMER)
                .build();

        assertEquals(AuthProvider.LOCAL, user.getAuthProvider());
        assertNull(user.getProviderId());
        assertNull(user.getAvatarUrl());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Una cuenta de Google conserva el identificador y el avatar")
    void googleAccountKeepsProviderData() {
        User user = User.builder()
                .email("google@pardrops.com")
                .password("hash")
                .fullName("Cuenta Google")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("google-sub-123")
                .avatarUrl("https://lh3.googleusercontent.com/a/foto")
                .build();

        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("google-sub-123", user.getProviderId());
        assertEquals("https://lh3.googleusercontent.com/a/foto", user.getAvatarUrl());
    }

    @Test
    @DisplayName("Al persistir se rellenan el proveedor, el estado y la fecha")
    void onCreateFillsDefaults() {
        User user = User.builder()
                .email("sin-proveedor@pardrops.com")
                .password("hash")
                .fullName("Sin proveedor")
                .role(Role.CUSTOMER)
                .authProvider(null)
                .status(null)
                .build();

        user.onCreate();

        assertEquals(AuthProvider.LOCAL, user.getAuthProvider());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getCreatedAt() != null);
    }
}
