package com.pardorps.pardrops.service;

import com.pardorps.pardrops.exception.GoogleLoginException;
import com.pardorps.pardrops.model.AuditAction;
import com.pardorps.pardrops.model.AuthProvider;
import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.model.UserStatus;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la creación y vinculación de cuentas de Google.
 */
class GoogleAuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final GoogleAuthService service = new GoogleAuthService(
            userRepository, passwordEncoder, auditLogService, notificationService);

    @Test
    @DisplayName("Crea una cuenta nueva con rol de cliente y proveedor Google")
    void createsNewGoogleAccount() {
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("nueva@pardrops.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash-tecnico");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });

        User user = service.resolveUser(googleUser("nueva@pardrops.com", "sub-1", true));

        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("sub-1", user.getProviderId());
        assertEquals(Role.CUSTOMER, user.getRole());
        assertEquals("https://foto.example/nueva", user.getAvatarUrl());
        assertNotNull(user.getPassword());
        verify(auditLogService).record(eq(user), eq(AuditAction.GOOGLE_ACCOUNT_CREATED),
                eq("USER"), eq("7"), any());
    }

    @Test
    @DisplayName("Vincula una cuenta local cuando Google confirma el correo")
    void linksLocalAccountWhenEmailIsVerified() {
        User local = User.builder()
                .id(3L)
                .email("local@pardrops.com")
                .password("hash-local")
                .fullName("Cuenta local")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-2"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("local@pardrops.com")).thenReturn(Optional.of(local));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.resolveUser(googleUser("local@pardrops.com", "sub-2", true));

        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
        assertEquals("sub-2", user.getProviderId());
        assertEquals("hash-local", user.getPassword());
        assertEquals("Cuenta local", user.getFullName());
        verify(auditLogService).record(eq(user), eq(AuditAction.GOOGLE_ACCOUNT_LINKED),
                eq("USER"), eq("3"), any());
        verify(notificationService).notify(eq(user), eq(NotificationType.ACCOUNT),
                any(), any(), eq("/cuenta"));
    }

    @Test
    @DisplayName("No vincula una cuenta si Google no confirma el correo")
    void refusesLinkWhenEmailIsNotVerified() {
        User local = User.builder()
                .id(4L)
                .email("dudoso@pardrops.com")
                .password("hash-local")
                .fullName("Cuenta local")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-3"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("dudoso@pardrops.com")).thenReturn(Optional.of(local));

        GoogleLoginException error = assertThrows(GoogleLoginException.class,
                () -> service.resolveUser(googleUser("dudoso@pardrops.com", "sub-3", false)));

        assertEquals("correo-no-verificado", error.getReason());
    }

    @Test
    @DisplayName("Rechaza una cuenta suspendida antes de emitir la sesión")
    void refusesSuspendedAccount() {
        User suspended = User.builder()
                .id(5L)
                .email("suspendida@pardrops.com")
                .password("hash")
                .fullName("Suspendida")
                .role(Role.CUSTOMER)
                .status(UserStatus.SUSPENDED)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("sub-4")
                .build();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-4"))
                .thenReturn(Optional.of(suspended));

        GoogleLoginException error = assertThrows(GoogleLoginException.class,
                () -> service.resolveUser(googleUser("suspendida@pardrops.com", "sub-4", true)));

        assertEquals("cuenta-suspendida", error.getReason());
    }

    @Test
    @DisplayName("Reutiliza la cuenta ya vinculada y actualiza el avatar")
    void reusesLinkedAccount() {
        User linked = User.builder()
                .id(6L)
                .email("vinculada@pardrops.com")
                .password("hash")
                .fullName("Nombre anterior")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .providerId("sub-5")
                .build();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-5"))
                .thenReturn(Optional.of(linked));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.resolveUser(googleUser("vinculada@pardrops.com", "sub-5", true));

        assertEquals(6L, user.getId());
        assertEquals("Nombre de Google", user.getFullName());
        assertEquals("https://foto.example/vinculada", user.getAvatarUrl());
    }

    @Test
    @DisplayName("Rechaza una identidad sin correo")
    void refusesIdentityWithoutEmail() {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getClaims()).thenReturn(Map.of("sub", "sub-6"));

        GoogleLoginException error = assertThrows(GoogleLoginException.class,
                () -> service.resolveUser(oidcUser));

        assertEquals("sin-correo", error.getReason());
    }

    @Test
    @DisplayName("Acepta el correo verificado aunque Google lo envíe como texto")
    void acceptsVerifiedEmailAsText() {
        User local = User.builder()
                .id(8L)
                .email("texto@pardrops.com")
                .password("hash-local")
                .fullName("Cuenta local")
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, "sub-7"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("texto@pardrops.com")).thenReturn(Optional.of(local));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getClaims()).thenReturn(Map.of(
                "email", "texto@pardrops.com",
                "sub", "sub-7",
                "name", "Cuenta local",
                "email_verified", "true"
        ));

        User user = service.resolveUser(oidcUser);

        assertEquals(AuthProvider.GOOGLE, user.getAuthProvider());
    }

    private OidcUser googleUser(String email, String sub, boolean verified) {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getClaims()).thenReturn(Map.of(
                "email", email,
                "sub", sub,
                "name", sub.equals("sub-5") ? "Nombre de Google"
                        : "Nombre " + sub,
                "picture", "https://foto.example/" + email.substring(0, email.indexOf('@')),
                "email_verified", verified
        ));
        return oidcUser;
    }
}
