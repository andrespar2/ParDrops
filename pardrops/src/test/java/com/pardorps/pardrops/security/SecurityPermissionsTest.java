package com.pardorps.pardrops.security;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración de las reglas de acceso de la API.
 *
 * <p>Levantan la aplicación en un puerto real y la llaman por HTTP, de modo
 * que se ejercita la cadena de seguridad completa. Comprueban que el backend
 * —y no solo la interfaz— impide a cada rol hacer lo que no le corresponde. En
 * concreto que un usuario autenticado sin permisos recibe un 403 y no un 401:
 * el 401 significa "falta sesión" y confundirlo con "no tienes permiso"
 * esconde el motivo real del rechazo.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityPermissionsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private RestClient restClient;

    /**
     * Prepara el cliente HTTP contra el servidor de pruebas.
     */
    @BeforeEach
    void setUp() {
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("Responde 401 cuando la petición no lleva sesión")
    void respondsUnauthorizedWithoutSession() {
        HttpStatus status = exchange(restClient.get().uri("/api/notifications"));

        assertEquals(HttpStatus.UNAUTHORIZED, status);
    }

    @Test
    @DisplayName("Responde 403 cuando un cliente entra al panel de administración")
    void respondsForbiddenWhenCustomerEntersAdminPanel() throws Exception {
        String token = registrar("cliente-admin@ejemplo.com", "Cliente Normal");

        HttpStatus status = exchange(
                restClient.get().uri("/api/admin/products").header("Authorization", "Bearer " + token));

        assertEquals(HttpStatus.FORBIDDEN, status);
    }

    @Test
    @DisplayName("Responde 403 cuando un vendedor entra al panel de administración")
    void respondsForbiddenWhenSellerEntersAdminPanel() throws Exception {
        String token = registrarComo("vendedor-admin@ejemplo.com", "Vendedor Normal", Role.SELLER);

        HttpStatus status = exchange(
                restClient.get().uri("/api/admin/users").header("Authorization", "Bearer " + token));

        assertEquals(HttpStatus.FORBIDDEN, status);
    }

    @Test
    @DisplayName("Responde 403 cuando el administrador entra al panel de vendedor")
    void respondsForbiddenWhenAdminEntersSellerPanel() throws Exception {
        String token = registrarComo("admin-panel@ejemplo.com", "Administrador Panel", Role.ADMIN);

        HttpStatus status = exchange(
                restClient.get().uri("/api/seller/products").header("Authorization", "Bearer " + token));

        assertEquals(HttpStatus.FORBIDDEN, status);
    }

    @Test
    @DisplayName("Permite al administrador consultar las cuentas de la plataforma")
    void allowsAdminToListUsers() throws Exception {
        String token = registrarComo("admin-lista@ejemplo.com", "Administrador Lista", Role.ADMIN);

        HttpStatus status = exchange(
                restClient.get().uri("/api/admin/users").header("Authorization", "Bearer " + token));

        assertEquals(HttpStatus.OK, status);
    }

    @Test
    @DisplayName("Deja que un cliente envíe su solicitud para vender")
    void allowsCustomerToApplyAsSeller() throws Exception {
        String token = registrar("cliente-vendedor@ejemplo.com", "Cliente Vendedor");

        HttpStatus status = exchange(
                restClient.post()
                        .uri("/api/seller/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "businessName": "Tienda de prueba",
                                  "description": "Prueba de integracion",
                                  "city": "Bogota"
                                }
                                """));

        assertEquals(HttpStatus.CREATED, status);
    }

    @Test
    @DisplayName("Convierte al cliente aprobado en vendedor con su tienda y su panel")
    void promotesApprovedApplicantToSeller() throws Exception {
        String tokenAdmin = registrarComo("admin-aprobacion@ejemplo.com", "Administrador Aprobacion", Role.ADMIN);
        String tokenCliente = registrar("cliente-aprobado@ejemplo.com", "Cliente Aprobado");

        String respuesta = exchangeConCuerpo(
                restClient.post()
                        .uri("/api/seller/applications")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "businessName": "Estilo Urbano de prueba",
                                  "description": "Prueba de integracion",
                                  "city": "Bogota"
                                }
                                """));

        long idSolicitud = objectMapper.readTree(respuesta).get("id").asLong();

        String revisada = exchangeConCuerpo(
                restClient.patch()
                        .uri("/api/admin/seller-applications/" + idSolicitud)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "approve": true,
                                  "reviewNotes": "Documentacion correcta"
                                }
                                """));

        assertEquals("APPROVED", objectMapper.readTree(revisada).get("status").asText());

        // El token sigue siendo valido: el rol se lee de la base de datos en
        // cada peticion, asi que el cliente aprobado ya usa el panel de vendedor.
        HttpStatus status = exchange(
                restClient.get().uri("/api/seller/products").header("Authorization", "Bearer " + tokenCliente));
        assertEquals(HttpStatus.OK, status);

        String tienda = exchangeConCuerpo(
                restClient.get().uri("/api/stores/mine").header("Authorization", "Bearer " + tokenCliente));
        assertEquals("estilo-urbano-de-prueba",
                objectMapper.readTree(tienda).get("slug").asText());
    }

    @Test
    @DisplayName("Publica el catálogo sin sesión y protege el panel de vendedor")
    void keepsCatalogPublicAndProtectsSellerPanel() {
        assertEquals(HttpStatus.OK, exchange(restClient.get().uri("/api/products")));
        assertEquals(HttpStatus.OK, exchange(restClient.get().uri("/api/stores")));
        assertEquals(HttpStatus.UNAUTHORIZED, exchange(restClient.get().uri("/api/seller/products")));
    }

    @Test
    @DisplayName("Devuelve un mensaje legible cuando falta el permiso")
    void returnsReadableMessageWhenAccessIsDenied() throws Exception {
        String token = registrar("cliente-mensaje@ejemplo.com", "Cliente Mensaje");

        String cuerpo = exchangeConCuerpoPermitiendoError(
                restClient.get().uri("/api/admin/products").header("Authorization", "Bearer " + token));

        JsonNode respuesta = objectMapper.readTree(cuerpo);
        assertTrue(respuesta.has("message"));
        assertNotNull(respuesta.get("message").asText());
    }

    /**
     * Registra un usuario nuevo y devuelve su token de sesión.
     *
     * @param email    correo de la cuenta.
     * @param fullName nombre de la cuenta.
     * @return token JWT de la sesión iniciada.
     * @throws Exception si la API responde un error inesperado.
     */
    private String registrar(String email, String fullName) throws Exception {
        String respuesta = exchangeConCuerpo(
                restClient.post()
                        .uri("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "email": "%s",
                                  "password": "clave12345",
                                  "fullName": "%s",
                                  "phone": "3000000000"
                                }
                                """.formatted(email, fullName)));

        return objectMapper.readTree(respuesta).get("token").asText();
    }

    /**
     * Registra un usuario y le asigna el rol indicado.
     *
     * <p>El alta pública siempre crea cuentas de cliente: por eso el rol de
     * administrador o de vendedor se asigna directamente en la base de datos,
     * que es exactamente lo que hace el panel de administración al aprobar una
     * solicitud.</p>
     *
     * @param email    correo de la cuenta.
     * @param fullName nombre de la cuenta.
     * @param role     rol que tendrá la cuenta.
     * @return token JWT de la sesión iniciada.
     * @throws Exception si la API responde un error inesperado.
     */
    private String registrarComo(String email, String fullName, Role role) throws Exception {
        String token = registrar(email, fullName);

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(role);
        userRepository.save(user);

        return token;
    }

    /**
     * Ejecuta la petición y devuelve el código de estado.
     *
     * @param request petición preparada.
     * @return código de estado de la respuesta.
     */
    private HttpStatus exchange(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((clientRequest, clientResponse) ->
                HttpStatus.valueOf(clientResponse.getStatusCode().value()));
    }

    /**
     * Ejecuta la petición y devuelve el cuerpo de la respuesta.
     *
     * @param request petición preparada.
     * @return cuerpo de la respuesta en texto.
     */
    private String exchangeConCuerpo(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((clientRequest, clientResponse) -> {
            String cuerpo = new String(clientResponse.getBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            if (clientResponse.getStatusCode().isError()) {
                throw new IllegalStateException(
                        "La peticion fallo con " + clientResponse.getStatusCode() + ": " + cuerpo);
            }
            return cuerpo;
        });
    }

    /**
     * Ejecuta la petición y devuelve el cuerpo aunque la respuesta sea un error.
     *
     * <p>Se usa para comprobar el mensaje que la API devuelve cuando rechaza
     * una operación sin permisos.</p>
     *
     * @param request petición preparada.
     * @return cuerpo de la respuesta en texto.
     */
    private String exchangeConCuerpoPermitiendoError(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((clientRequest, clientResponse) ->
                new String(clientResponse.getBody().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8));
    }
}
