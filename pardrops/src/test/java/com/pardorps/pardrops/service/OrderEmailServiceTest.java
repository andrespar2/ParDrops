package com.pardorps.pardrops.service;

import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderItem;
import com.pardorps.pardrops.model.OrderStatus;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.Store;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/**
 * Pruebas del correo que recibe el vendedor al crearse un pedido.
 */
class OrderEmailServiceTest {

    private final EmailTemplateService templateService = org.mockito.Mockito.mock(EmailTemplateService.class);
    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
    private final StoreRepository storeRepository = org.mockito.Mockito.mock(StoreRepository.class);
    private final OrderEmailService service = new OrderEmailService(
            templateService, emailService, storeRepository, "http://localhost:4200");

    @Test
    @DisplayName("Envía al vendedor el correo de su subpedido")
    void sendsSellerOrderEmail() {
        User seller = seller(7L, "vendedor@pardrops.com");
        Store store = store(seller, "contacto@tienda.com");
        OrderSubOrder subOrder = subOrder(seller, "Tienda Uno");
        Order order = order(subOrder);

        org.mockito.Mockito.when(storeRepository.findBySellerId(7L)).thenReturn(Optional.of(store));
        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.SELLER_NEW_ORDER),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Asunto", "<p>Hola</p>")));

        service.sendNewOrderToSellers(order);

        verify(emailService).send("contacto@tienda.com", "Asunto", "<p>Hola</p>");
    }

    @Test
    @DisplayName("Usa el correo de la cuenta si la tienda no tiene uno válido")
    void fallsBackToSellerAccountEmail() {
        User seller = seller(8L, "vendedor@pardrops.com");
        Store store = store(seller, null);
        OrderSubOrder subOrder = subOrder(seller, "Tienda Dos");
        Order order = order(subOrder);

        org.mockito.Mockito.when(storeRepository.findBySellerId(8L)).thenReturn(Optional.of(store));
        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.SELLER_NEW_ORDER),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Asunto", "<p>Hola</p>")));

        service.sendNewOrderToSellers(order);

        verify(emailService).send("vendedor@pardrops.com", "Asunto", "<p>Hola</p>");
    }

    @Test
    @DisplayName("No envía nada para un subpedido de la plataforma")
    void skipsPlatformSubOrder() {
        OrderSubOrder subOrder = subOrder(null, "ParDrops");
        Order order = order(subOrder);

        service.sendNewOrderToSellers(order);

        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("No envía si la plantilla está inactiva")
    void skipsInactiveTemplate() {
        User seller = seller(9L, "vendedor@pardrops.com");
        Store store = store(seller, "contacto@tienda.com");
        OrderSubOrder subOrder = subOrder(seller, "Tienda Tres");
        Order order = order(subOrder);

        org.mockito.Mockito.when(storeRepository.findBySellerId(9L)).thenReturn(Optional.of(store));
        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.empty());

        service.sendNewOrderToSellers(order);

        org.mockito.Mockito.verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Envía el comprobante del pedido completo al comprador")
    void sendsPaymentReceiptForWholeOrder() {
        OrderSubOrder subOrder = subOrder(seller(11L, "vendedor@pardrops.com"), "Tienda");
        Order order = order(subOrder);

        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.BUYER_PAYMENT_RECEIPT),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Comprobante", "<p>Pago</p>")));

        service.sendPaymentReceipt(order);

        verify(emailService).send("ana@correo.com", "Comprobante", "<p>Pago</p>");
    }

    @Test
    @DisplayName("Envía el comprobante de una parte al comprador")
    void sendsPaymentReceiptForSubOrder() {
        OrderSubOrder subOrder = subOrder(seller(12L, "vendedor@pardrops.com"), "Tienda");
        Order order = order(subOrder);

        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.BUYER_PAYMENT_RECEIPT),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Comprobante", "<p>Pago</p>")));

        service.sendPaymentReceipt(order, subOrder);

        verify(emailService).send("ana@correo.com", "Comprobante", "<p>Pago</p>");
    }

    @Test
    @DisplayName("Envía el aviso de estado del pedido completo")
    void sendsStatusUpdateForWholeOrder() {
        OrderSubOrder subOrder = subOrder(seller(13L, "vendedor@pardrops.com"), "Tienda");
        Order order = order(subOrder);
        order.setStatus(OrderStatus.PREPARED);

        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.BUYER_ORDER_STATUS),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Estado", "<p>Preparado</p>")));

        service.sendStatusUpdate(order);

        verify(emailService).send("ana@correo.com", "Estado", "<p>Preparado</p>");
    }

    @Test
    @DisplayName("Envía el aviso de estado de una parte del pedido")
    void sendsStatusUpdateForSubOrder() {
        OrderSubOrder subOrder = subOrder(seller(14L, "vendedor@pardrops.com"), "Tienda");
        Order order = order(subOrder);
        subOrder.setStatus(OrderStatus.SHIPPED);

        org.mockito.Mockito.when(templateService.renderIfActive(
                        org.mockito.ArgumentMatchers.eq(com.pardorps.pardrops.model.EmailTemplateCode.BUYER_ORDER_STATUS),
                        org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(Optional.of(new com.pardorps.pardrops.dto.RenderedEmail("Estado", "<p>Enviado</p>")));

        service.sendStatusUpdate(order, subOrder);

        verify(emailService).send("ana@correo.com", "Estado", "<p>Enviado</p>");
    }

    @Test
    @DisplayName("Calcula el IVA repartido y formatea los importes")
    void calculatesTaxAndFormatsMoney() {
        OrderSubOrder subOrder = subOrder(null, "ParDrops");
        subOrder.setSubtotal(100_000.0);
        subOrder.setShipping(25_900.0);
        subOrder.setTotal(144_900.0);

        assertEquals(19_000.0, service.tax(subOrder), 0.01);
        assertEquals("$ 100.000", service.money(100_000.0));
        assertEquals("Pagado", service.statusLabel(OrderStatus.PAID));
    }

    private Order order(OrderSubOrder subOrder) {
        Order order = new Order();
        order.setId(12L);
        order.setCustomerName("Ana");
        order.setCustomerEmail("ana@correo.com");
        order.setCustomerPhone("3000000000");
        order.setAddressStreet("Calle 1");
        order.setAddressCity("Bogotá");
        order.setAddressPostalCode("110111");
        order.setAddressCountry("Colombia");
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.addSubOrder(subOrder);
        return order;
    }

    private OrderSubOrder subOrder(User seller, String storeName) {
        OrderSubOrder subOrder = new OrderSubOrder();
        subOrder.setId(3L);
        subOrder.setSeller(seller);
        subOrder.setStoreName(storeName);
        subOrder.setStatus(OrderStatus.PENDING);
        subOrder.setSubtotal(100_000.0);
        subOrder.setShipping(25_900.0);
        subOrder.setTotal(144_900.0);
        subOrder.setCreatedAt(Instant.now());

        OrderItem item = new OrderItem();
        item.setProductName("Camiseta");
        item.setQuantity(1);
        item.setUnitPrice(100_000.0);
        item.setSubtotal(100_000.0);
        subOrder.addItem(item);
        return subOrder;
    }

    private User seller(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName("Vendedor")
                .role(Role.SELLER)
                .build();
    }

    private Store store(User seller, String contactEmail) {
        Store store = new Store();
        store.setId(1L);
        store.setSeller(seller);
        store.setName("Tienda");
        store.setSlug("tienda");
        store.setContactEmail(contactEmail);
        return store;
    }
}
