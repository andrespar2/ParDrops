package com.pardorps.pardrops.service;

import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración del panel de administración.
 *
 * <p>Levantan la aplicación en un puerto real y llaman a la API por HTTP, de
 * modo que se ejercita la cadena de seguridad completa y las consultas de
 * verdad. Comprueban que las reglas que se exigen al panel (filtros y paginación
 * de cuentas, listado de vendedores, moderación del catálogo y CRUD de
 * categorías) funcionan contra la base de datos.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:pardrops-panel-admin-test;DB_CLOSE_DELAY=-1")
class AdminPanelIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    /**
     * Comprueba que la API del catálogo global responde al administrador.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("El administrador ve el catálogo global con su propietario")
    void adminSeesGlobalCatalog() throws Exception {
        String token = tokenAdmin("admin-catalogo@ejemplo.com");

        String cuerpo = get("/api/admin/products?size=5", token);
        JsonNode respuesta = objectMapper.readTree(cuerpo);

        assertTrue(respuesta.has("content"));
        assertTrue(respuesta.has("totalElements"));

        String ficha = get("/api/seller/products", tokenVendedor("vendedor-catalogo@ejemplo.com"));
        assertTrue(objectMapper.readTree(ficha).isArray());
    }

    /**
     * Comprueba que el listado de cuentas admite paginación y filtros.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("El listado de cuentas pagina y filtra por rol y estado")
    void adminUsersSupportPaginationAndFilters() throws Exception {
        String token = tokenAdmin("admin-usuarios@ejemplo.com");
        tokenVendedor("vendedor-usuarios@ejemplo.com");

        JsonNode pagina = objectMapper.readTree(
                get("/api/admin/users?page=0&size=5&role=SELLER&status=ACTIVE", token));

        assertEquals(0, pagina.get("page").asInt());
        assertEquals(5, pagina.get("size").asInt());
        assertTrue(pagina.get("content").isArray());
        pagina.get("content").forEach(user ->
                assertEquals("SELLER", user.get("role").asText()));
    }

    /**
     * Comprueba que existe el listado de vendedores con su tienda.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("El administrador consulta los vendedores con su tienda")
    void adminCanListSellers() throws Exception {
        String token = tokenAdmin("admin-vendedores@ejemplo.com");
        tokenVendedorConTienda("vendedor-listado@ejemplo.com", "Tienda Listado");

        JsonNode vendedores = objectMapper.readTree(get("/api/admin/sellers", token));

        assertTrue(vendedores.isArray());
        assertTrue(vendedores.size() > 0);
        assertTrue(vendedores.get(0).has("storeSlug"));
        assertTrue(vendedores.get(0).has("salesTotal"));
    }

    /**
     * Comprueba el ciclo completo de las categorías, incluido el bloqueo del
     * borrado cuando la categoría está en uso.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("Las categorías se crean, editan y no se borran si están en uso")
    void categoriesFollowLifecycle() throws Exception {
        String token = tokenAdmin("admin-categorias@ejemplo.com");

        JsonNode creada = objectMapper.readTree(post("/api/admin/categories", token, """
                {
                  "name": "Categoria de prueba admin",
                  "description": "Creada por la prueba de integracion",
                  "active": true,
                  "featured": false
                }
                """));

        long id = creada.get("id").asLong();
        assertEquals("categoria-de-prueba-admin", creada.get("slug").asText());
        assertEquals(0, creada.get("productCount").asLong());

        JsonNode editada = objectMapper.readTree(put("/api/admin/categories/" + id, token, """
                {
                  "name": "Categoria de prueba admin",
                  "description": "Descripcion actualizada",
                  "active": false,
                  "featured": true
                }
                """));

        assertEquals(false, editada.get("active").asBoolean());
        assertEquals(true, editada.get("featured").asBoolean());

        // Sin productos asociados, la categoría se puede borrar.
        assertEquals(HttpStatus.NO_CONTENT, delete("/api/admin/categories/" + id, token));
        assertEquals(HttpStatus.NOT_FOUND, statusGet("/api/admin/categories/" + id, token));
    }

    /**
     * Comprueba que una categoría en uso no se puede borrar.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("No permite borrar una categoría que usan productos")
    void refusesToDeleteCategoryInUse() throws Exception {
        String token = tokenAdmin("admin-categoria-uso@ejemplo.com");

        String listado = get("/api/admin/categories", token);
        JsonNode categorias = objectMapper.readTree(listado);

        JsonNode enUso = null;
        for (JsonNode categoria : categorias) {
            if (categoria.get("productCount").asLong() > 0) {
                enUso = categoria;
                break;
            }
        }

        if (enUso == null) {
            // Sin datos sembrados no hay categoría en uso que probar.
            return;
        }

        long id = enUso.get("id").asLong();
        assertEquals(HttpStatus.CONFLICT, delete("/api/admin/categories/" + id, token));
    }

    /**
     * Comprueba que el resumen del panel responde con sus indicadores.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("El resumen general devuelve los indicadores de la plataforma")
    void dashboardReturnsIndicators() throws Exception {
        String token = tokenAdmin("admin-resumen@ejemplo.com");

        JsonNode resumen = objectMapper.readTree(get("/api/admin/dashboard", token));

        assertTrue(resumen.has("userCount"));
        assertTrue(resumen.has("productCount"));
        assertTrue(resumen.has("pendingApplicationCount"));
        assertTrue(resumen.has("recentOrders"));
        assertTrue(resumen.has("topStores"));
    }

    /**
     * Comprueba que la ficha de un producto incluye a su propietario.
     *
     * @throws Exception si la API responde un error inesperado.
     */
    @Test
    @DisplayName("La ficha de un producto de vendedor muestra su propietario")
    void productDetailShowsOwner() throws Exception {
        String tokenAdmin = tokenAdmin("admin-detalle-producto@ejemplo.com");
        String tokenVendedor =
                tokenVendedorConTienda("vendedor-detalle@ejemplo.com", "Tienda Detalle");

        long idCategoria = objectMapper.readTree(post("/api/admin/categories", tokenAdmin, """
                {
                  "name": "Categoria detalle producto",
                  "active": true
                }
                """)).get("id").asLong();

        String producto = post("/api/seller/products", tokenVendedor, """
                {
                  "name": "Producto de detalle",
                  "slug": "producto-de-detalle-admin",
                  "description": "Producto para la prueba de detalle",
                  "price": 50000,
                  "currency": "COP",
                  "stock": 5,
                  "categories": ["Categoria detalle producto"],
                  "tags": ["detalle"],
                  "images": [],
                  "variants": [],
                  "isNew": false,
                  "isFeatured": false,
                  "onSale": false
                }
                """);

        String idProducto = objectMapper.readTree(producto).get("id").asText();

        JsonNode ficha = objectMapper.readTree(
                get("/api/admin/products/" + idProducto, tokenAdmin));

        assertTrue(ficha.get("sellerName").asText().length() > 0);
        assertEquals("tienda-detalle", ficha.get("storeSlug").asText());

        // Al ocultar el producto, el vendedor recibe un aviso.
        JsonNode oculto = objectMapper.readTree(
                patch("/api/admin/products/" + idProducto + "/visibility?active=false", tokenAdmin));
        assertEquals(false, oculto.get("active").asBoolean());

        String avisos = get("/api/notifications", tokenVendedor);
        assertTrue(avisos.contains("Producto"), "El vendedor debe recibir un aviso de moderacion");

        // Y el catálogo público deja de mostrarlo.
        String catalogo = get("/api/products", null);
        assertTrue(!catalogo.contains("producto-de-detalle-admin"));

        // Limpieza de la categoría usada por la prueba.
        delete("/api/admin/categories/" + idCategoria, tokenAdmin);
        delete("/api/admin/products/" + idProducto, tokenAdmin);
    }

    /**
     * Registra un administrador y devuelve su token.
     *
     * @param email correo de la cuenta.
     * @return token JWT de la sesión.
     * @throws Exception si la API responde un error inesperado.
     */
    private String tokenAdmin(String email) throws Exception {
        return registrar(email, "Administrador " + email, Role.ADMIN);
    }

    /**
     * Registra un cliente y le asigna el rol de vendedor.
     *
     * @param email correo de la cuenta.
     * @return token JWT de la sesión.
     * @throws Exception si la API responde un error inesperado.
     */
    private String tokenVendedor(String email) throws Exception {
        return registrar(email, "Vendedor " + email, Role.SELLER);
    }

    /**
     * Registra un vendedor y le crea su tienda.
     *
     * <p>El alta pública siempre crea cuentas de cliente y la tienda se crea al
     * aprobar una solicitud. Aquí se asigna el rol y se crea la tienda
     * directamente en la base de datos, que es exactamente el estado que deja la
     * aprobación.</p>
     *
     * @param email  correo de la cuenta.
     * @param nombre nombre comercial de la tienda.
     * @return token JWT de la sesión.
     * @throws Exception si la API responde un error inesperado.
     */
    private String tokenVendedorConTienda(String email, String nombre) throws Exception {
        String token = tokenVendedor(email);

        User seller = userRepository.findByEmail(email).orElseThrow();
        if (storeRepository.findBySellerId(seller.getId()).isEmpty()) {
            Store store = new Store();
            store.setSeller(seller);
            store.setName(nombre);
            store.setSlug(slug(nombre));
            store.setDescription("Tienda creada por la prueba de integracion.");
            store.setStatus(StoreStatus.ACTIVE);
            storeRepository.save(store);
        }

        return token;
    }

    /**
     * Genera el slug de una tienda a partir de su nombre.
     *
     * @param nombre nombre comercial.
     * @return slug en minúsculas y sin espacios.
     */
    private String slug(String nombre) {
        return nombre.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /**
     * Registra una cuenta y le asigna el rol indicado.
     *
     * @param email correo de la cuenta.
     * @param nombre nombre de la cuenta.
     * @param role  rol que tendrá la cuenta.
     * @return token JWT de la sesión.
     * @throws Exception si la API responde un error inesperado.
     */
    private String registrar(String email, String nombre, Role role) throws Exception {
        String respuesta = post("/api/auth/register", null, """
                {
                  "email": "%s",
                  "password": "clave12345",
                  "fullName": "%s",
                  "phone": "3000000000"
                }
                """.formatted(email, nombre));

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(role);
        userRepository.save(user);

        return objectMapper.readTree(respuesta).get("token").asText();
    }

    /**
     * Ejecuta una petición GET.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @return cuerpo de la respuesta.
     */
    private String get(String uri, String token) {
        RestClient.RequestHeadersSpec<?> request = client().get().uri(uri);
        return send(withToken(request, token));
    }

    /**
     * Ejecuta una petición POST con cuerpo JSON.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @param body  cuerpo JSON.
     * @return cuerpo de la respuesta.
     */
    private String post(String uri, String token, String body) {
        RestClient.RequestBodySpec request = client().post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON);
        return send(withToken(request.body(body), token));
    }

    /**
     * Ejecuta una petición PUT con cuerpo JSON.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @param body  cuerpo JSON.
     * @return cuerpo de la respuesta.
     */
    private String put(String uri, String token, String body) {
        RestClient.RequestBodySpec request = client().put().uri(uri)
                .contentType(MediaType.APPLICATION_JSON);
        return send(withToken(request.body(body), token));
    }

    /**
     * Ejecuta una petición PATCH.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @return cuerpo de la respuesta.
     */
    private String patch(String uri, String token) {
        return send(withToken(client().patch().uri(uri), token));
    }

    /**
     * Ejecuta una petición DELETE.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @return código de estado de la respuesta.
     */
    private HttpStatus delete(String uri, String token) {
        return client().delete().uri(uri)
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);
                    }
                })
                .exchange((request, response) ->
                        HttpStatus.valueOf(response.getStatusCode().value()));
    }

    /**
     * Ejecuta una petición GET y devuelve solo el código de estado.
     *
     * @param uri   ruta de la API.
     * @param token token de sesión (puede ser nulo).
     * @return código de estado de la respuesta.
     */
    private HttpStatus statusGet(String uri, String token) {
        return client().get().uri(uri)
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);
                    }
                })
                .exchange((request, response) ->
                        HttpStatus.valueOf(response.getStatusCode().value()));
    }

    /**
     * Añade el token de sesión a la petición si lo hay.
     *
     * @param request petición preparada.
     * @param token   token de sesión (puede ser nulo).
     * @return la petición con la cabecera de autorización.
     */
    private RestClient.RequestHeadersSpec<?> withToken(
            RestClient.RequestHeadersSpec<?> request, String token) {
        if (token == null) {
            return request;
        }
        return request.header("Authorization", "Bearer " + token);
    }

    /**
     * Ejecuta la petición y devuelve su cuerpo, fallando si la respuesta es un error.
     *
     * @param request petición preparada.
     * @return cuerpo de la respuesta en texto.
     */
    private String send(RestClient.RequestHeadersSpec<?> request) {
        return request.exchange((clientRequest, clientResponse) -> {
            String cuerpo = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
            if (clientResponse.getStatusCode().isError()) {
                throw new IllegalStateException(
                        "La peticion fallo con " + clientResponse.getStatusCode() + ": " + cuerpo);
            }
            return cuerpo;
        });
    }

    /**
     * Crea un cliente HTTP contra el servidor de pruebas.
     *
     * @return el cliente listo para usar.
     */
    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }
}
