# Notificaciones

## Tipos de notificación

Definidos en `NotificationType`:

| Tipo | Uso | Destinatario típico |
|---|---|---|
| `INFO` | Información general. | Cualquier usuario autenticado. |
| `ORDER` | Relacionadas con pedidos (nueva venta, avance de estado). | Vendedor (nueva venta) / Cliente (avance de estado). |
| `PRODUCT` | Cambios en productos (visibilidad). | Vendedor propietario. |
| `SELLER_APPLICATION` | Ciclo de solicitud para vender. | Solicitante / Administradores. |
| `ACCOUNT` | Cambios de cuenta (suspensión/reactivación). | Usuario afectado. |

## Flujos de notificación

| Evento | Tipo | Título (ejemplo) | Mensaje | Enlace |
|---|---|---|---|---|
| Solicitud enviada | `SELLER_APPLICATION` | "Solicitud registrada" | "Recibimos tu solicitud para vender en ParDrops..." | — |
| Nueva solicitud (admin) | `SELLER_APPLICATION` | "Nueva solicitud de vendedor" | "{nombre} quiere vender en ParDrops: {negocio}" | `/admin/solicitudes` |
| Solicitud aprobada | `SELLER_APPLICATION` | "¡Tu solicitud fue aprobada!" | "Ya puedes publicar productos en tu tienda..." | `/vendedor` |
| Solicitud rechazada | `SELLER_APPLICATION` | "Tu solicitud no fue aprobada" | Motivo de rechazo (si existe) | — |
| Pedido creado (vendedor) | `ORDER` | "Tienes una venta nueva" | "La tienda {nombre} tiene un pedido por preparar." | `/vendedor/pedidos` |
| Pedido creado (cliente) | `ORDER` | "Tu pedido está en camino" | "Hemos registrado tu pedido. Te avisaremos cuando cada tienda lo envíe." | `/cuenta` |
| Avance de subpedido (cliente) | `ORDER` | "Tu pedido va avanzando" | "La tienda {nombre} actualizó su parte del pedido a {estado}." | `/cuenta` |
| Producto oculto/publicado | `PRODUCT` | "Tu producto fue oculto" / "Tu producto vuelve a estar publicado" | Información sobre visibilidad. | `/vendedor/productos` |
| Cuenta suspendida/reactivada | `ACCOUNT` | "Tu cuenta fue suspendida" / "Tu cuenta fue reactivada" | Motivo si existe / mensaje informativo. | — |

## API y frontend

- **Backend**: `NotificationController` (`/api/notifications`), `NotificationService`. Métodos: listar, contar no leídas, marcar como leída, marcar todas como leídas.
- **Frontend**: `NotificationService`, `NotificationBellComponent` en cabecera, página `/notificaciones`. El panel del vendedor también muestra las **últimas notificaciones** del resumen.
- **Persistencia**: `notifications` con `user_id`, `type`, `title`, `message`, `link`, `read`, `created_at`.
