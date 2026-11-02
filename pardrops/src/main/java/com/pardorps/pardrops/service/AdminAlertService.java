package com.pardorps.pardrops.service;

import com.pardorps.pardrops.model.NotificationType;
import com.pardorps.pardrops.model.Order;
import com.pardorps.pardrops.model.OrderSubOrder;
import com.pardorps.pardrops.model.Role;
import com.pardorps.pardrops.model.User;
import com.pardorps.pardrops.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Avisos operativos para administración y usuarios.
 *
 * <p>Combina la notificación interna (persistente) con el correo electrónico.
 * Si el correo falla, la notificación interna se conserva.</p>
 */
@Service
public class AdminAlertService {

    private static final Logger log = LoggerFactory.getLogger(AdminAlertService.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    /**
     * @param userRepository      acceso a las cuentas.
     * @param notificationService notificaciones internas.
     * @param emailService        envío de correo.
     */
    public AdminAlertService(
            UserRepository userRepository,
            NotificationService notificationService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    /**
     * Avisa a todos los administradores de una venta confirmada.
     *
     * @param order pedido creado.
     */
    public void newSale(Order order) {
        String sellers = order.getSubOrders().stream()
                .map(OrderSubOrder::getStoreName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        String message = "Pedido " + order.getId() + " · " + order.getItems().size()
                + " producto(s) · Total " + order.getTotal()
                + (sellers.isBlank() ? "" : " · Vendedores: " + sellers);

        notifyAdmins("Nueva venta en ParDrops", message, "/admin/pedidos");
    }

    /**
     * Notifica a un usuario por los dos canales.
     *
     * @param user    destinatario.
     * @param type    tipo de notificación.
     * @param title   título.
     * @param message mensaje.
     * @param link    enlace interno.
     */
    public void notifyUser(User user, NotificationType type, String title, String message, String link) {
        notificationService.notify(user, type, title, message, link);
        sendEmail(user.getEmail(), title, message, link);
    }

    /**
     * Notifica a todos los administradores.
     *
     * @param title   título.
     * @param message mensaje.
     * @param link    enlace al panel.
     */
    public void notifyAdmins(String title, String message, String link) {
        for (User admin : userRepository.findAll()) {
            if (admin.getRole() == Role.ADMIN && admin.getEmail() != null) {
                notificationService.notify(admin, NotificationType.ORDER, title, message, link);
                sendEmail(admin.getEmail(), title, message, link);
            }
        }
    }

    /** Envía el correo sin romper la operación si falla. */
    private void sendEmail(String to, String title, String message, String link) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            boolean sent = emailService.send(to, title, html(title, message, link));
            if (!sent) {
                log.warn("No se pudo enviar el correo '{}' a {}", title, to);
            }
        } catch (RuntimeException exception) {
            log.warn("Error enviando correo '{}': {}", title, exception.getMessage());
        }
    }

    /** Plantilla HTML sencilla con la identidad de ParDrops. */
    private String html(String title, String message, String link) {
        String url = link == null ? "" : "<p style=\"margin:24px 0 0\">"
                + "<a href=\"" + link + "\" style=\"background:#111827;color:#ffffff;padding:10px 18px;"
                + "border-radius:999px;text-decoration:none;font-weight:600\">Abrir en ParDrops</a></p>";
        return "<div style=\"font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;background:#f8fafc;"
                + "padding:32px\"><div style=\"max-width:560px;margin:0 auto;background:#ffffff;"
                + "border:1px solid #e5e7eb;border-radius:16px;padding:28px\">"
                + "<p style=\"margin:0 0 4px;font-size:13px;letter-spacing:.08em;text-transform:uppercase;"
                + "color:#6b7280\">ParDrops</p>"
                + "<h1 style=\"margin:0 0 12px;font-size:22px;color:#111827\">" + title + "</h1>"
                + "<p style=\"margin:0;font-size:15px;line-height:1.6;color:#374151\">" + message + "</p>"
                + url
                + "</div></div>";
    }
}
