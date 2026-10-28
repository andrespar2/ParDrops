package com.pardorps.pardrops.mail;

import java.util.HashMap;
import java.util.Map;

/**
 * Datos de ejemplo para la vista previa de las plantillas.
 *
 * <p>No tocan la base de datos: reproducen un pedido verosímil para que el
 * administrador vea el correo tal y como llegaría.</p>
 */
public final class EmailPreviewData {

    private EmailPreviewData() {
    }

    /**
     * Construye el modelo de ejemplo.
     *
     * @return variables de la plantilla.
     */
    public static Map<String, Object> model() {
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", "1024");
        model.put("orderDate", "28/10/2026 10:30");
        model.put("customerName", "Ana Gómez");
        model.put("customerEmail", "ana@ejemplo.com");
        model.put("customerPhone", "300 123 4567");
        model.put("address", "Calle 10 # 20-30, Bogotá, 110111, Colombia");
        model.put("notes", "Dejar en portería");
        model.put("storeName", "Estilo Urbano");
        model.put("sellerName", "Jorge Pardo");
        model.put("itemsTable", new EmailTemplateRenderer.SafeHtml(itemsTable()));
        model.put("subtotal", "$ 100.000");
        model.put("tax", "$ 19.000");
        model.put("shipping", "$ 25.900");
        model.put("total", "$ 144.900");
        model.put("status", "PREPARED");
        model.put("statusLabel", "Preparado");
        model.put("accountUrl", "http://localhost:4200/cuenta");
        model.put("sellerOrdersUrl", "http://localhost:4200/vendedor/pedidos");
        return model;
    }

    private static String itemsTable() {
        return """
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="width:100%;border-collapse:collapse;font-size:14px;color:#374151;">
                  <thead>
                    <tr style="background:#f9fafb;">
                      <th align="left" style="padding:10px 8px;font-size:12px;color:#6b7280;">Producto</th>
                      <th align="center" style="padding:10px 8px;font-size:12px;color:#6b7280;">Cant.</th>
                      <th align="right" style="padding:10px 8px;font-size:12px;color:#6b7280;">Precio</th>
                      <th align="right" style="padding:10px 8px;font-size:12px;color:#6b7280;">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr style="border-top:1px solid #e5e7eb;">
                      <td style="padding:10px 8px;">Camiseta oversize</td>
                      <td align="center" style="padding:10px 8px;">1</td>
                      <td align="right" style="padding:10px 8px;">$ 100.000</td>
                      <td align="right" style="padding:10px 8px;">$ 100.000</td>
                    </tr>
                  </tbody>
                </table>
                """;
    }
}
