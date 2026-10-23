package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.CreateOrderRequest;
import com.pardorps.pardrops.dto.OrderItemRequest;
import com.pardorps.pardrops.dto.OrderResponse;
import com.pardorps.pardrops.exception.ApiException;
import com.pardorps.pardrops.model.Notification;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.NotificationRepository;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integración del registro de un pedido multivendedor.
 *
 * <p>Unlike las pruebas con dobles, aquí se usa la base de datos real: de esa
 * forma se comprueba que el descuento de stock es transaccional y que, si el
 * pedido falla, la operación entera se deshace.</p>
 */
@SpringBootTest
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:pardrops-multivendedor-test;DB_CLOSE_DELAY=-1")
class OrderMultivendorIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSubOrderRepository subOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User sellerA;
    private User sellerB;
    private User customer;

    /**
     * Prepara dos vendedores con su tienda y un cliente.
     *
     * <p>Cada prueba usa correos propios: el correo es único en la base de
     * datos, así que dos pruebas no pueden compartir la misma cuenta.</p>
     *
     * @param testInfo datos de la prueba en curso.
     */
    @BeforeEach
    void setUp(TestInfo testInfo) {
        String sufijo = testInfo.getTestMethod().orElseThrow().getName();

        sellerA = guardarUsuario("vendedor-a-" + sufijo + "@multivendedor.test", "Vendedor A", Role.SELLER);
        sellerB = guardarUsuario("vendedor-b-" + sufijo + "@multivendedor.test", "Vendedor B", Role.SELLER);
        customer = guardarUsuario("cliente-" + sufijo + "@multivendedor.test", "Cliente Multivendedor", Role.CUSTOMER);

        guardarTienda(sellerA, "Estilo Urbano", "estilo-urbano-" + sufijo);
        guardarTienda(sellerB, "Calzado Deportivo", "calzado-deportivo-" + sufijo);
    }

    @Test
    @DisplayName("Guarda un subpedido por vendedor con los importes repartidos")
    void persistsOneSubOrderPerSeller() {
        Product gorra = guardarProducto("gorra-multivendedor", "Gorra urbana", 100_000.0, 10, sellerA);
        Product zapatillas = guardarProducto("zapatillas-multivendedor", "Zapatillas", 50_000.0, 8, sellerB);

        OrderResponse response = orderService.createOrder(
                peticion(List.of(linea(gorra.getId(), 1), linea(zapatillas.getId(), 1))),
                customer);

        assertEquals(2, response.subOrders().size());
        assertEquals(2, subOrderRepository.findByOrderId(response.id()).size());
        assertEquals(OrderStatus.PENDING, response.status());

        // Los importes de las partes suman exactamente el total del pedido.
        double suma = response.subOrders().stream().mapToDouble(parte -> parte.total()).sum();
        assertEquals(response.total(), suma, 0.001);

        // Cada parte conserva solo las líneas de su tienda.
        assertEquals(1, response.subOrders().get(0).items().size());
        assertEquals("Gorra urbana", response.subOrders().get(0).items().get(0).productName());
        assertEquals(1, response.subOrders().get(1).items().size());
        assertEquals("Zapatillas", response.subOrders().get(1).items().get(0).productName());

        // El stock se descuenta una sola vez por unidad.
        assertEquals(9, productRepository.findById(gorra.getId()).orElseThrow().getStock());
        assertEquals(7, productRepository.findById(zapatillas.getId()).orElseThrow().getStock());

        // Cada vendedor recibe su aviso y el cliente también.
        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(sellerA.getId()).size());
        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(sellerB.getId()).size());
        List<Notification> avisos = notificationRepository.findByUserIdOrderByCreatedAtDesc(customer.getId());
        assertEquals(1, avisos.size());
    }

    @Test
    @DisplayName("No descuenta el stock si el pedido no se puede registrar")
    void doesNotDecreaseStockWhenOrderFails() {
        Product gorra = guardarProducto("gorra-sin-stock", "Gorra sin stock", 100_000.0, 2, sellerA);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> orderService.createOrder(peticion(List.of(linea(gorra.getId(), 5))), customer)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals(2, productRepository.findById(gorra.getId()).orElseThrow().getStock());
        assertTrue(orderRepository.findAll().stream()
                .noneMatch(pedido -> pedido.getItems().stream()
                        .anyMatch(linea -> linea.getProductId().equals(gorra.getId()))));
    }

    /**
     * Guarda un usuario de prueba.
     *
     * @param email    correo de la cuenta.
     * @param fullName nombre de la cuenta.
     * @param role     rol de la cuenta.
     * @return el usuario guardado.
     */
    private User guardarUsuario(String email, String fullName, Role role) {
        User user = User.builder()
                .email(email)
                .password("hash")
                .fullName(fullName)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Guarda la tienda de un vendedor.
     *
     * @param seller propietario de la tienda.
     * @param name   nombre comercial.
     * @param slug   slug público.
     */
    private void guardarTienda(User seller, String name, String slug) {
        Store store = new Store();
        store.setSeller(seller);
        store.setName(name);
        store.setSlug(slug);
        store.setStatus(StoreStatus.ACTIVE);
        storeRepository.save(store);
    }

    /**
     * Guarda un producto del catálogo.
     *
     * @param id     identificador del producto.
     * @param name   nombre del producto.
     * @param price  precio unitario.
     * @param stock  unidades disponibles.
     * @param seller vendedor del producto.
     * @return el producto guardado.
     */
    private Product guardarProducto(String id, String name, double price, int stock, User seller) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSlug(id);
        product.setDescription("Descripción de prueba");
        product.setPrice(price);
        product.setStock(stock);
        product.setSeller(seller);
        product.setActive(true);
        return productRepository.save(product);
    }

    /**
     * Construye una petición de compra.
     *
     * @param items líneas del pedido.
     * @return la petición lista para usar.
     */
    private CreateOrderRequest peticion(List<OrderItemRequest> items) {
        return new CreateOrderRequest(
                "Cliente Multivendedor",
                "cliente@multivendedor.test",
                "+57 300 000 0000",
                "Calle 1 # 2-3",
                "Bogotá",
                "110111",
                "Colombia",
                null,
                items
        );
    }

    /**
     * Construye una línea de pedido.
     *
     * @param productId producto comprado.
     * @param quantity  unidades compradas.
     * @return la línea lista para usar.
     */
    private OrderItemRequest linea(String productId, int quantity) {
        return new OrderItemRequest(productId, null, null, quantity);
    }
}
