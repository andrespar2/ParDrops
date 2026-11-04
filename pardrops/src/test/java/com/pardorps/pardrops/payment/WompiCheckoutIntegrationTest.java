package com.pardorps.pardrops.payment;

import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.PaymentEmailKind;
import com.pardorps.pardrops.model.PaymentEmailStatus;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.PlatformRevenueType;
import com.pardorps.pardrops.model.Product;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.StoreStatus;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.NotificationRepository;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.OrderSubOrderRepository;
import com.pardorps.pardrops.repository.PaymentEmailLogRepository;
import com.pardorps.pardrops.repository.PlatformRevenueRepository;
import com.pardorps.pardrops.repository.ProductRepository;
import com.pardorps.pardrops.repository.SellerBalanceRepository;
import com.pardorps.pardrops.repository.StoreRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Pruebas de integración del checkout y del webhook de Wompi.
 *
 * <p>Se usa H2 y un cliente de Wompi simulado: las firmas, los cálculos, la
 * idempotencia, los saldos, los avisos y los correos se ejercitan de verdad,
 * pero ninguna transacción real ni dinero real interviene.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pardrops-wompi-test;DB_CLOSE_DELAY=-1",
        "app.wompi.env=sandbox",
        "app.wompi.currency=COP",
        "app.wompi.fixed-fee-cop=700",
        "app.wompi.test.public-key=pub_test_CLAVEDEPRUEBASPARADROPS",
        "app.wompi.test.private-key=prv_test_CLAVEDEPRUEBASPARADROPS",
        "app.wompi.test.events-secret=test_events_SECRETODEPRUEBAS",
        "app.wompi.test.integrity-secret=test_integrity_SECRETODEPRUEBAS"
})
class WompiCheckoutIntegrationTest {

    private static final long TIMESTAMP = 1_730_000_000L;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderSubOrderRepository subOrderRepository;

    @Autowired
    private SellerBalanceRepository balanceRepository;

    @Autowired
    private PlatformRevenueRepository revenueRepository;

    @Autowired
    private PaymentEmailLogRepository emailLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WompiSignatureService signatureService;

    @MockitoBean
    private WompiClient wompiClient;

    private RestClient client;
    private User seller;
    private User buyer;
    private User admin;
    private Product product;
    private String suffix;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        client = RestClient.builder().baseUrl("http://localhost:" + port).build();
        suffix = Integer.toHexString(
                testInfo.getTestMethod().orElseThrow().getName().hashCode());

        seller = user("vendedor-" + suffix + "@wompi.test", "Vendedor Wompi", Role.SELLER);
        buyer = user("comprador-" + suffix + "@wompi.test", "Comprador Wompi", Role.CUSTOMER);
        admin = user("admin-" + suffix + "@wompi.test", "Admin Wompi", Role.ADMIN);

        Store store = new Store();
        store.setSeller(seller);
        store.setName("Tienda " + suffix);
        store.setSlug("tienda-" + suffix);
        store.setStatus(StoreStatus.ACTIVE);
        store.setContactEmail("tienda-" + suffix + "@wompi.test");
        storeRepository.save(store);

        product = new Product();
        product.setId("prod-" + suffix);
        product.setName("Producto " + suffix);
        product.setSlug("prod-" + suffix);
        product.setDescription("Producto de prueba");
        product.setPrice(50_000.0);
        product.setStock(10);
        product.setSeller(seller);
        product.setActive(true);
        product.setRequiresShipping(true);
        product.setShippingCost(8_000.0);
        product.setFreeShippingThreshold(400_000.0);
        productRepository.save(product);
    }

    @Test
    @DisplayName("El checkout usa el precio de la base de datos y cobra 700 una sola vez")
    void createsCheckoutWithDatabasePriceAndSingleServiceFee() {
        JsonNode checkout = checkout();

        assertEquals(58_700.0, checkout.path("total").asDouble(), 0.001);
        assertEquals(50_000.0, checkout.path("subtotal").asDouble(), 0.001);
        assertEquals(8_000.0, checkout.path("shipping").asDouble(), 0.001);
        assertEquals(700.0, checkout.path("serviceFee").asDouble(), 0.001);
        assertEquals(5_870_000L, checkout.path("amountInCents").asLong());
        assertEquals("COP", checkout.path("currency").asText());
        assertEquals("test", checkout.path("environment").asText());

        String reference = checkout.path("reference").asText();
        assertEquals(
                signatureService.integritySignature(
                        reference, 5_870_000L, "COP", "test_integrity_SECRETODEPRUEBAS"),
                checkout.path("integritySignature").asText());

        Order order = orderRepository.findByPaymentReference(reference).orElseThrow();
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(700.0, order.getServiceFee(), 0.001);
        // El stock no se descuenta hasta que el pago se aprueba.
        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getStock());
        assertTrue(balanceRepository.findBySellerId(seller.getId()).isEmpty());
        assertTrue(emailLogRepository.findByOrderId(order.getId()).isEmpty());
    }

    @Test
    @DisplayName("Un evento aprobado confirma la compra, retiene saldos y envía los tres correos")
    void approvesPurchaseAndDistributesFunds() {
        JsonNode checkout = checkout();
        String reference = checkout.path("reference").asText();
        long orderId = checkout.path("orderId").asLong();
        long amount = checkout.path("amountInCents").asLong();

        String transactionId = "tx-approved-" + suffix;
        when(wompiClient.fetchTransaction(transactionId)).thenReturn(
                new WompiTransaction(transactionId, reference, "APPROVED", amount,
                        "COP", "CARD", buyer.getEmail(), "Aprobada", Instant.now()));

        HttpStatus status = postEvent(transactionId, reference, "APPROVED", amount, true);
        assertEquals(HttpStatus.OK, status);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(PaymentStatus.APPROVED, order.getPaymentStatus());
        assertEquals("PAID", order.getStatus().name());
        assertEquals(transactionId, order.getWompiTransactionId());
        assertNotNull(order.getPaidAt());

        // Neto del vendedor: 50.000 + 8.000 de envío - 2.500 de comisión.
        assertEquals(55_500.0,
                balanceRepository.findBySellerId(seller.getId()).orElseThrow().getPendingAmount(), 0.001);
        assertEquals(0.0,
                balanceRepository.findBySellerId(seller.getId()).orElseThrow().getAvailableAmount(), 0.001);

        // El cargo de 700 se registra como ingreso de la plataforma una sola vez.
        var revenue = revenueRepository
                .findByOrderIdAndType(orderId, PlatformRevenueType.SERVICE_FEE).orElseThrow();
        assertEquals(700.0, revenue.getAmount(), 0.001);

        // Avisos internos: vendedor y administrador. El comprador de esta
        // prueba es invitado (sin cuenta), así que no tiene bandeja interna;
        // su confirmación llega por correo. Un comprador autenticado sí recibe
        // el aviso, como comprueba PaymentApplicationService.
        assertTrue(notificationRepository.findByUserIdOrderByCreatedAtDesc(seller.getId()).size() >= 1);
        assertTrue(notificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId()).size() >= 1);

        // Correos personalizados registrados como enviados.
        var logs = emailLogRepository.findByOrderId(orderId);
        assertTrue(logs.stream().anyMatch(log -> log.getKind() == PaymentEmailKind.BUYER
                && log.getStatus() == PaymentEmailStatus.SENT));
        assertTrue(logs.stream().anyMatch(log -> log.getKind() == PaymentEmailKind.SELLER
                && log.getStatus() == PaymentEmailStatus.SENT
                && log.getRecipient().equals("tienda-" + suffix + "@wompi.test")));
        assertTrue(logs.stream().anyMatch(log -> log.getKind() == PaymentEmailKind.ADMIN
                && log.getStatus() == PaymentEmailStatus.SENT));

        // Reintento del mismo evento: no duplica saldos, ingresos, avisos ni correos.
        int notificationsBefore = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(seller.getId()).size();
        int logsBefore = logs.size();
        assertEquals(HttpStatus.OK, postEvent(transactionId, reference, "APPROVED", amount, true));
        assertEquals(55_500.0,
                balanceRepository.findBySellerId(seller.getId()).orElseThrow().getPendingAmount(), 0.001);
        assertEquals(700.0, revenueRepository
                .findByOrderIdAndType(orderId, PlatformRevenueType.SERVICE_FEE).orElseThrow()
                .getAmount(), 0.001);
        assertEquals(notificationsBefore, notificationRepository
                .findByUserIdOrderByCreatedAtDesc(seller.getId()).size());
        assertEquals(logsBefore, emailLogRepository.findByOrderId(orderId).size());
    }

    @Test
    @DisplayName("Un evento con firma incorrecta no confirma la compra")
    void rejectsInvalidChecksum() {
        JsonNode checkout = checkout();
        String reference = checkout.path("reference").asText();
        long orderId = checkout.path("orderId").asLong();

        HttpStatus status = postEvent("tx-invalid", reference, "APPROVED",
                checkout.path("amountInCents").asLong(), false);

        assertEquals(HttpStatus.BAD_REQUEST, status);
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertTrue(balanceRepository.findBySellerId(seller.getId()).isEmpty());
        assertTrue(revenueRepository.findByOrderIdAndType(
                orderId, PlatformRevenueType.SERVICE_FEE).isEmpty());
    }

    @Test
    @DisplayName("Un pago rechazado no acredita saldos ni envía correos de compra")
    void recordsDeclinedPaymentWithoutFunds() {
        JsonNode checkout = checkout();
        String reference = checkout.path("reference").asText();
        long orderId = checkout.path("orderId").asLong();
        long amount = checkout.path("amountInCents").asLong();
        String transactionId = "tx-declined-" + suffix;

        when(wompiClient.fetchTransaction(transactionId)).thenReturn(
                new WompiTransaction(transactionId, reference, "DECLINED", amount,
                        "COP", "CARD", buyer.getEmail(), "Rechazada", Instant.now()));

        assertEquals(HttpStatus.OK, postEvent(transactionId, reference, "DECLINED", amount, true));

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertEquals(PaymentStatus.DECLINED, order.getPaymentStatus());
        assertEquals("PENDING", order.getStatus().name());
        assertTrue(balanceRepository.findBySellerId(seller.getId()).isEmpty());
        assertTrue(emailLogRepository.findByOrderId(orderId).isEmpty());
        assertTrue(revenueRepository.findByOrderIdAndType(
                orderId, PlatformRevenueType.SERVICE_FEE).isEmpty());
    }

    @Test
    @DisplayName("Si el vendedor cambia el precio antes de pagar, el pedido cobra el nuevo precio")
    void usesUpdatedPriceOnNextCheckout() {
        checkout();

        product.setPrice(72_000.0);
        productRepository.save(product);

        JsonNode second = checkout();
        assertEquals(72_000.0, second.path("subtotal").asDouble(), 0.001);
        assertEquals(700.0, second.path("serviceFee").asDouble(), 0.001);
    }

    @Test
    @DisplayName("El reintento de un pago rechazado conserva el pedido y no duplica el cargo")
    void retryKeepsOrderAndFee() {
        JsonNode checkout = checkout();
        String reference = checkout.path("reference").asText();
        long orderId = checkout.path("orderId").asLong();
        long amount = checkout.path("amountInCents").asLong();
        String transactionId = "tx-retry-" + suffix;

        when(wompiClient.fetchTransaction(transactionId)).thenReturn(
                new WompiTransaction(transactionId, reference, "DECLINED", amount,
                        "COP", "CARD", buyer.getEmail(), "Rechazada", Instant.now()));
        assertEquals(HttpStatus.OK, postEvent(transactionId, reference, "DECLINED", amount, true));

        JsonNode retry = client.post()
                .uri("/api/checkout/{reference}/retry", reference)
                .retrieve()
                .body(JsonNode.class);

        assertNotNull(retry);
        assertEquals(orderId, retry.path("orderId").asLong());
        assertEquals(58_700.0, retry.path("total").asDouble(), 0.001);
        assertEquals(700.0, retry.path("serviceFee").asDouble(), 0.001);
        assertTrue(!reference.equals(retry.path("reference").asText()));
        assertEquals(1, orderRepository.findAll().stream()
                .filter(order -> order.getId() == orderId)
                .count());
    }

    @Test
    @DisplayName("Una compra con dos vendedores reparte saldos y envía un correo a cada uno")
    void approvesMultivendorPurchaseAndSendsEmailPerSeller() {
        User secondSeller = user("vendedor2-" + suffix + "@wompi.test", "Segundo Vendedor", Role.SELLER);
        Store secondStore = new Store();
        secondStore.setSeller(secondSeller);
        secondStore.setName("Segunda Tienda " + suffix);
        secondStore.setSlug("segunda-tienda-" + suffix);
        secondStore.setStatus(StoreStatus.ACTIVE);
        secondStore.setContactEmail("segunda-" + suffix + "@wompi.test");
        storeRepository.save(secondStore);

        Product second = new Product();
        second.setId("prod2-" + suffix);
        second.setName("Producto dos " + suffix);
        second.setSlug("prod2-" + suffix);
        second.setDescription("Producto de prueba");
        second.setPrice(30_000.0);
        second.setStock(10);
        second.setSeller(secondSeller);
        second.setActive(true);
        second.setRequiresShipping(true);
        second.setShippingCost(5_000.0);
        second.setFreeShippingThreshold(400_000.0);
        productRepository.save(second);

        JsonNode checkout = checkout(product.getId(), second.getId());
        String reference = checkout.path("reference").asText();
        long orderId = checkout.path("orderId").asLong();
        long amount = checkout.path("amountInCents").asLong();

        assertEquals(2, subOrderRepository.findByOrderId(orderId).size());

        String transactionId = "tx-multivendor-" + suffix;
        when(wompiClient.fetchTransaction(transactionId)).thenReturn(
                new WompiTransaction(transactionId, reference, "APPROVED", amount,
                        "COP", "CARD", buyer.getEmail(), "Aprobada", Instant.now()));
        assertEquals(HttpStatus.OK, postEvent(transactionId, reference, "APPROVED", amount, true));

        // 50.000 - 2.500 de comisión + 8.000 de envío.
        assertEquals(55_500.0,
                balanceRepository.findBySellerId(seller.getId()).orElseThrow().getPendingAmount(), 0.001);
        // 30.000 - 1.500 de comisión + 5.000 de envío.
        assertEquals(33_500.0,
                balanceRepository.findBySellerId(secondSeller.getId()).orElseThrow()
                        .getPendingAmount(), 0.001);

        var sellerLogs = emailLogRepository.findByOrderId(orderId).stream()
                .filter(log -> log.getKind() == PaymentEmailKind.SELLER)
                .toList();
        assertEquals(2, sellerLogs.size());
    }

    private JsonNode checkout() {
        return checkout(product.getId());
    }

    private JsonNode checkout(String... productIds) {
        StringBuilder items = new StringBuilder();
        for (int index = 0; index < productIds.length; index++) {
            if (index > 0) {
                items.append(',');
            }
            items.append("{\"productId\":\"").append(productIds[index])
                    .append("\",\"quantity\":1}");
        }

        String body = """
                {
                  "customerName": "Comprador Wompi",
                  "customerEmail": "%s",
                  "customerPhone": "3000000000",
                  "addressStreet": "Calle 1 # 2-3",
                  "addressCity": "Bogotá",
                  "addressPostalCode": "110111",
                  "addressCountry": "Colombia",
                  "notes": "Prueba",
                  "items": [%s]
                }
                """.formatted(buyer.getEmail(), items);

        return client.post()
                .uri("/api/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    private HttpStatus postEvent(
            String transactionId,
            String reference,
            String status,
            long amountInCents,
            boolean validChecksum
    ) {
        String raw = """
                {
                  "event": "transaction.updated",
                  "data": {
                    "transaction": {
                      "id": "%s",
                      "reference": "%s",
                      "status": "%s",
                      "amount_in_cents": %d,
                      "currency": "COP",
                      "payment_method_type": "CARD",
                      "customer_email": "%s"
                    }
                  },
                  "environment": "test",
                  "signature": {
                    "properties": ["transaction.id", "transaction.status", "transaction.amount_in_cents"],
                    "checksum": "__CHECKSUM__"
                  },
                  "timestamp": %d,
                  "sent_at": "2026-01-01T00:00:00.000Z"
                }
                """.formatted(transactionId, reference, status, amountInCents, buyer.getEmail(), TIMESTAMP);

        JsonNode parsed = mapper.readTree(raw);
        String checksum = signatureService.eventChecksum(
                parsed.path("data"),
                List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"),
                TIMESTAMP,
                "test_events_SECRETODEPRUEBAS");
        if (!validChecksum) {
            checksum = "0000000000000000000000000000000000000000000000000000000000000000";
        }
        String body = raw.replace("__CHECKSUM__", checksum);

        return client.post()
                .uri("/api/webhooks/wompi")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Event-Checksum", checksum)
                .body(body)
                .exchange((request, response) -> HttpStatus.valueOf(response.getStatusCode().value()));
    }

    private User user(String email, String name, Role role) {
        return userRepository.save(User.builder()
                .email(email)
                .password("hash")
                .fullName(name)
                .role(role)
                .build());
    }
}
