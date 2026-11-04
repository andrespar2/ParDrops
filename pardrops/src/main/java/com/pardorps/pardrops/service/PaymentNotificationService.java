package com.pardorps.pardrops.service;

import com.pardorps.pardrops.dto.RenderedEmail;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.PaymentEmailKind;
import com.pardorps.pardrops.model.PaymentStatus;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.OrderRepository;
import com.pardorps.pardrops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Envía los tres correos personalizados de una compra confirmada.
 *
 * <p>Se ejecuta únicamente después de que el pago quedó aprobado y la
 * transacción de base de datos terminó correctamente. Un fallo de correo se
 * registra y nunca revierte la compra.</p>
 */
@Service
public class PaymentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationService.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderEmailService orderEmailService;
    private final PaymentEmailDispatchService dispatchService;

    /**
     * @param orderRepository    acceso a los pedidos.
     * @param userRepository     acceso a las cuentas administrativas.
     * @param orderEmailService  renderizado de las plantillas.
     * @param dispatchService    envío y registro de cada correo.
     */
    public PaymentNotificationService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            OrderEmailService orderEmailService,
            PaymentEmailDispatchService dispatchService
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderEmailService = orderEmailService;
        this.dispatchService = dispatchService;
    }

    /**
     * Envía los correos de comprador, vendedores y administradores.
     *
     * @param orderId pedido confirmado.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void sendAllEmails(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("No se encontró el pedido {} para enviar los correos", orderId);
            return;
        }
        if (!PaymentStatus.APPROVED.equals(order.getPaymentStatus())) {
            log.warn("El pedido {} no está aprobado; no se envían correos de compra", orderId);
            return;
        }

        sendBuyer(order);
        sendSellers(order);
        sendAdmins(order);
    }

    private void sendBuyer(Order order) {
        Optional<RenderedEmail> rendered = orderEmailService.renderBuyerPurchaseConfirmed(order);
        if (rendered.isEmpty()) {
            log.info("Plantilla de comprador inactiva para el pedido {}", order.getId());
            return;
        }
        dispatchService.sendOnce(
                order.getId(),
                PaymentEmailKind.BUYER,
                order.getCustomerEmail(),
                rendered.get());
    }

    private void sendSellers(Order order) {
        for (OrderSubOrder subOrder : order.getSubOrders()) {
            if (subOrder.getSeller() == null) {
                continue;
            }
            String recipient = orderEmailService.sellerEmail(subOrder);
            if (recipient == null || recipient.isBlank()) {
                log.warn("Subpedido {} sin correo de vendedor", subOrder.getId());
                continue;
            }
            Optional<RenderedEmail> rendered =
                    orderEmailService.renderSellerSaleConfirmed(order, subOrder);
            if (rendered.isEmpty()) {
                log.info("Plantilla de vendedor inactiva para el pedido {}", order.getId());
                continue;
            }
            dispatchService.sendOnce(
                    order.getId(),
                    PaymentEmailKind.SELLER,
                    recipient,
                    rendered.get());
        }
    }

    private void sendAdmins(Order order) {
        Optional<RenderedEmail> rendered = orderEmailService.renderAdminSaleConfirmed(order);
        if (rendered.isEmpty()) {
            log.info("Plantilla de administración inactiva para el pedido {}", order.getId());
            return;
        }
        List<User> admins = userRepository.findAll();
        for (User admin : admins) {
            if (admin.getRole() != Role.ADMIN || admin.getEmail() == null || admin.getEmail().isBlank()) {
                continue;
            }
            dispatchService.sendOnce(
                    order.getId(),
                    PaymentEmailKind.ADMIN,
                    admin.getEmail(),
                    rendered.get());
        }
    }
}
