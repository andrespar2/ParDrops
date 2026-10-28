package com.pardorps.pardrops.mail;

import com.pardorps.pardrops.model.EmailTemplateCode;

import java.util.List;

/**
 * Plantillas de correo que se crean en el primer arranque.
 *
 * <p>El administrador puede editarlas después desde el panel. Estas versiones
 * solo se insertan cuando la plantilla no existe y sirven de restauración.</p>
 */
public final class EmailTemplateDefaults {

    private EmailTemplateDefaults() {
    }

    /**
     * Plantilla predeterminada de un tipo de correo.
     *
     * @param code        código.
     * @param name        nombre visible.
     * @param description cuándo se envía.
     * @param subject     asunto.
     * @param bodyHtml    cuerpo HTML.
     */
    public record DefaultTemplate(
            EmailTemplateCode code,
            String name,
            String description,
            String subject,
            String bodyHtml
    ) {
    }

    /**
     * Devuelve las tres plantillas predeterminadas.
     *
     * @return listado de plantillas.
     */
    public static List<DefaultTemplate> all() {
        return List.of(sellerNewOrder(), buyerPaymentReceipt(), buyerOrderStatus());
    }

    /**
     * Busca la plantilla predeterminada de un código.
     *
     * @param code código buscado.
     * @return la plantilla predeterminada.
     */
    public static DefaultTemplate of(EmailTemplateCode code) {
        return all().stream()
                .filter(template -> template.code() == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Plantilla sin valores predeterminados: " + code));
    }

    private static DefaultTemplate sellerNewOrder() {
        String content = """
                <p style="margin:0 0 16px;font-size:16px;color:#111827;">Hola, {{sellerName}}:</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#374151;">
                  Tienes un pedido nuevo en <strong>{{storeName}}</strong>. Prepara los productos
                  y mantén actualizado el estado para que el comprador pueda seguirlo.
                </p>

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:0 0 20px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Pedido</td>
                    <td style="padding:8px 0;text-align:right;font-weight:600;">#{{orderId}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Fecha</td>
                    <td style="padding:8px 0;text-align:right;">{{orderDate}}</td>
                  </tr>
                </table>

                <h2 style="margin:0 0 12px;font-size:16px;color:#111827;">Productos</h2>
                {{itemsTable}}

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:8px 0 24px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">Subtotal</td>
                    <td style="padding:6px 0;text-align:right;">{{subtotal}}</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">IVA</td>
                    <td style="padding:6px 0;text-align:right;">{{tax}}</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">Envío</td>
                    <td style="padding:6px 0;text-align:right;">{{shipping}}</td>
                  </tr>
                  <tr>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;font-weight:700;">Total de tu parte</td>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;text-align:right;font-weight:700;">{{total}}</td>
                  </tr>
                </table>

                <h2 style="margin:0 0 12px;font-size:16px;color:#111827;">Datos del comprador</h2>
                <p style="margin:0 0 6px;font-size:14px;color:#374151;">
                  <strong>{{customerName}}</strong><br>
                  {{customerEmail}}<br>
                  {{customerPhone}}
                </p>
                <p style="margin:0 0 24px;font-size:14px;line-height:1.6;color:#374151;">
                  <strong>Dirección de envío</strong><br>
                  {{address}}
                </p>

                <a href="{{sellerOrdersUrl}}"
                   style="display:inline-block;padding:12px 20px;border-radius:8px;background:#252525;color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">
                  Ver el pedido en el panel
                </a>
                """;
        return new DefaultTemplate(
                EmailTemplateCode.SELLER_NEW_ORDER,
                "Nuevo pedido para el vendedor",
                "Se envía al vendedor cuando se crea un pedido con productos de su tienda.",
                "Nuevo pedido #{{orderId}} en {{storeName}}",
                layout(content));
    }

    private static DefaultTemplate buyerPaymentReceipt() {
        String content = """
                <p style="margin:0 0 16px;font-size:16px;color:#111827;">Hola, {{customerName}}:</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#374151;">
                  Registramos el pago de tu pedido <strong>#{{orderId}}</strong> en ParDrops.
                  Este es tu comprobante de compra.
                </p>

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:0 0 20px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Pedido</td>
                    <td style="padding:8px 0;text-align:right;font-weight:600;">#{{orderId}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Fecha</td>
                    <td style="padding:8px 0;text-align:right;">{{orderDate}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Tienda</td>
                    <td style="padding:8px 0;text-align:right;">{{storeName}}</td>
                  </tr>
                </table>

                <h2 style="margin:0 0 12px;font-size:16px;color:#111827;">Detalle de la compra</h2>
                {{itemsTable}}

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:8px 0 24px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">Subtotal</td>
                    <td style="padding:6px 0;text-align:right;">{{subtotal}}</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">IVA</td>
                    <td style="padding:6px 0;text-align:right;">{{tax}}</td>
                  </tr>
                  <tr>
                    <td style="padding:6px 0;color:#6b7280;">Envío</td>
                    <td style="padding:6px 0;text-align:right;">{{shipping}}</td>
                  </tr>
                  <tr>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;font-weight:700;">Total pagado</td>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;text-align:right;font-weight:700;">{{total}}</td>
                  </tr>
                </table>

                <p style="margin:0 0 24px;font-size:14px;line-height:1.6;color:#374151;">
                  <strong>Dirección de envío</strong><br>
                  {{address}}
                </p>

                <p style="margin:0 0 24px;font-size:13px;line-height:1.6;color:#6b7280;">
                  Este comprobante confirma el registro del pago en ParDrops. No sustituye
                  a la factura electrónica.
                </p>

                <a href="{{accountUrl}}"
                   style="display:inline-block;padding:12px 20px;border-radius:8px;background:#252525;color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">
                  Ver mi cuenta
                </a>
                """;
        return new DefaultTemplate(
                EmailTemplateCode.BUYER_PAYMENT_RECEIPT,
                "Comprobante de compra",
                "Se envía al comprador cuando el pedido pasa a pagado.",
                "Comprobante de compra — Pedido #{{orderId}}",
                layout(content));
    }

    private static DefaultTemplate buyerOrderStatus() {
        String content = """
                <p style="margin:0 0 16px;font-size:16px;color:#111827;">Hola, {{customerName}}:</p>
                <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#374151;">
                  Tu pedido <strong>#{{orderId}}</strong> cambió a
                  <strong>{{statusLabel}}</strong>.
                </p>

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:0 0 20px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Pedido</td>
                    <td style="padding:8px 0;text-align:right;font-weight:600;">#{{orderId}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Fecha</td>
                    <td style="padding:8px 0;text-align:right;">{{orderDate}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Tienda</td>
                    <td style="padding:8px 0;text-align:right;">{{storeName}}</td>
                  </tr>
                  <tr>
                    <td style="padding:8px 0;color:#6b7280;">Estado</td>
                    <td style="padding:8px 0;text-align:right;font-weight:600;">{{statusLabel}}</td>
                  </tr>
                </table>

                <h2 style="margin:0 0 12px;font-size:16px;color:#111827;">Resumen</h2>
                {{itemsTable}}

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="margin:8px 0 24px;font-size:14px;color:#374151;">
                  <tr>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;font-weight:700;">Total</td>
                    <td style="padding:12px 0 0;border-top:1px solid #e5e7eb;text-align:right;font-weight:700;">{{total}}</td>
                  </tr>
                </table>

                <a href="{{accountUrl}}"
                   style="display:inline-block;padding:12px 20px;border-radius:8px;background:#252525;color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">
                  Ver mi pedido
                </a>
                """;
        return new DefaultTemplate(
                EmailTemplateCode.BUYER_ORDER_STATUS,
                "Cambio de estado del pedido",
                "Se envía al comprador en cada cambio de estado distinto de pagado.",
                "Tu pedido #{{orderId}} está {{statusLabel}}",
                layout(content));
    }

    /**
     * Envuelve el contenido en el diseño común.
     *
     * @param content contenido HTML del correo.
     * @return documento HTML completo.
     */
    private static String layout(String content) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>ParDrops</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;">
                    <tr>
                      <td align="center" style="padding:32px 16px;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                               style="max-width:600px;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;">
                          <tr>
                            <td style="padding:24px 32px;border-bottom:1px solid #e5e7eb;">
                              <span style="font-family:Georgia,'Times New Roman',serif;font-size:26px;font-weight:700;color:#111827;">ParDrops</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                """ + content + """
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px;background:#f9fafb;border-top:1px solid #e5e7eb;">
                              <p style="margin:0;font-size:12px;line-height:1.6;color:#6b7280;">
                                ParDrops · Este correo se envió automáticamente. Si no reconoces esta
                                compra, responde a este mensaje para que podamos ayudarte.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;
    }
}
