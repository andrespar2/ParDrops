# Correos transaccionales

Diseño de las notificaciones por correo de ParDrops y de sus plantillas
editables. Forma parte del proyecto
[ParDrops - Notificaciones por correo](https://github.com/users/andrespar2/projects/4).

## 1. Alcance

Se envían tres correos:

| Código | Destinatario | Momento |
|---|---|---|
| `SELLER_NEW_ORDER` | Vendedor | Al crear el pedido, con su subpedido |
| `BUYER_PAYMENT_RECEIPT` | Comprador | Al pasar el pedido a `PAID` |
| `BUYER_ORDER_STATUS` | Comprador | En cada cambio de estado distinto de `PAID` |

Las plantillas se pueden editar desde **Administración → Plantillas de correo**.

## 2. Decisión sobre el pago

La plataforma **no tiene pasarela de pago**. El checkout crea el pedido en
`PENDING` y el estado `PAID` se registra cuando el administrador o el vendedor
lo marcan. Por tanto:

- El comprobante se envía en la transición a `PAID`, que es el único punto de
  verdad disponible hoy.
- El comprobante aclara que el pago fue **registrado en la plataforma**, sin
  inventar referencias, métodos ni pasarelas.
- Cuando se añada una pasarela real, bastará con publicar el mismo evento desde
  el nuevo servicio de pago.

No se cambia el flujo de estados ni se bloquean los saltos actuales.

## 3. Flujo

```text
Checkout
  └─ POST /api/orders
       ├─ se guarda el pedido en PENDING
       ├─ por cada subpedido con vendedor:
       │    └─ correo SELLER_NEW_ORDER al vendedor
       └─ el comprador no recibe correo todavía

Admin o vendedor cambia el estado
  ├─ PAID
  │    └─ correo BUYER_PAYMENT_RECEIPT al comprador
  ├─ PREPARED / SHIPPED / DELIVERED / CANCELLED
  │    └─ correo BUYER_ORDER_STATUS al comprador
  └─ si el estado no cambia, no se envía nada
```

Cuando el administrador cambia el estado global, se envía **un solo correo
agregado** al comprador, no uno por cada subpedido.

## 4. Destinatarios

| Rol | Fuente | Respaldo |
|---|---|---|
| Vendedor | `Store.contactEmail` si es válido | `OrderSubOrder.seller.email` |
| Comprador | `Order.customerEmail` | Ninguno; es obligatorio |

`customerEmail` se usa también en compras de invitado. No se usa
`Order.user.email` porque puede ser distinto del correo escrito en el checkout.

Los subpedidos de plataforma (`seller == null`) no generan correo al vendedor.

## 5. Plantillas predeterminadas

### 5.1 `SELLER_NEW_ORDER`

**Asunto**

```text
Nuevo pedido #{{orderId}} en {{storeName}}
```

**Cuerpo**

- Saludo: «Hola, {{sellerName}}: tienes un pedido nuevo en {{storeName}}».
- Pedido `#{{orderId}}`, fecha `{{orderDate}}` y estado actual.
- Tabla de productos: producto, variante, cantidad, precio unitario y subtotal.
- Subtotal, IVA, envío y total de la parte del vendedor.
- Datos del comprador: nombre, correo y teléfono.
- Dirección de envío completa.
- Notas del cliente, si existen.
- Botón «Ver el pedido en el panel» → `{{sellerOrdersUrl}}`.

### 5.2 `BUYER_PAYMENT_RECEIPT`

**Asunto**

```text
Comprobante de compra — Pedido #{{orderId}}
```

**Cuerpo**

- Saludo y agradecimiento: «Hola, {{customerName}}: registramos el pago de tu
  pedido #{{orderId}}».
- Fecha del pedido y tienda(s).
- Tabla de productos con cantidades e importes.
- Subtotal, IVA, envío y total.
- Dirección de envío.
- Texto: «Este comprobante confirma el registro del pago en ParDrops. No
  sustituye a la factura electrónica.»
- Botón «Ver mi cuenta» → `{{accountUrl}}`.

### 5.3 `BUYER_ORDER_STATUS`

**Asunto**

```text
Tu pedido #{{orderId}} está {{statusLabel}}
```

**Cuerpo**

- Saludo.
- Estado nuevo y tienda que lo actualizó.
- Resumen del pedido y total.
- Botón «Ver mi pedido» → `{{accountUrl}}`.

## 6. Variables

| Variable | Contenido |
|---|---|
| `{{orderId}}` | Número del pedido |
| `{{orderDate}}` | Fecha del pedido en formato colombiano |
| `{{customerName}}` | Nombre del comprador |
| `{{customerEmail}}` | Correo del comprador |
| `{{customerPhone}}` | Teléfono del comprador |
| `{{address}}` | Dirección completa en una línea |
| `{{storeName}}` | Tienda del subpedido |
| `{{sellerName}}` | Nombre del vendedor |
| `{{itemsTable}}` | Tabla HTML de productos |
| `{{subtotal}}` | Subtotal |
| `{{tax}}` | IVA |
| `{{shipping}}` | Envío |
| `{{total}}` | Total |
| `{{status}}` | Código del estado (`PAID`, `SHIPPED`…) |
| `{{statusLabel}}` | Estado legible en español |
| `{{accountUrl}}` | Enlace a `/cuenta` |
| `{{sellerOrdersUrl}}` | Enlace a `/vendedor/pedidos` |

Los valores de usuario se escapan antes de sustituirse. El HTML de la plantilla
lo controla el administrador.

## 7. Envío

### 7.1 Modos

| Modo | Cuándo | Comportamiento |
|---|---|---|
| SMTP | `app.mail.enabled=true` | Envía con `JavaMailSender` |
| Outbox | `false` (por defecto en desarrollo) | Guarda un `.eml` en `./data/mail-outbox` |

El outbox permite revisar el correo generado sin credenciales ni conexión.

### 7.2 Fallos

El envío se hace **después** de guardar el pedido o el cambio de estado y dentro
de un `try/catch`. Un fallo SMTP:

- No revierte el pedido.
- No cambia el estado.
- Se registra en el log con el destinatario y la plantilla.

### 7.3 Por qué no asíncrono en esta fase

El envío es síncrono para que el resultado sea determinista y fácil de probar.
La interfaz `MailGateway` permite sustituirlo por una cola o por envío
asíncrono más adelante sin tocar los servicios de pedidos.

## 8. Plantillas editables

### Modelo

| Campo | Tipo |
|---|---|
| `id` | Long |
| `code` | Enum único (`SELLER_NEW_ORDER`, `BUYER_PAYMENT_RECEIPT`, `BUYER_ORDER_STATUS`) |
| `name` | Texto |
| `description` | Texto |
| `subject` | Texto |
| `bodyHtml` | Texto largo |
| `active` | Booleano |
| `createdAt` / `updatedAt` | Instant |

### Arranque

Un seeder idempotente crea las tres plantillas que falten. Nunca sobrescribe una
plantilla editada.

### Administración

| Método | Ruta | Acceso |
|---|---|---|
| `GET` | `/api/admin/email-templates` | ADMIN |
| `GET` | `/api/admin/email-templates/{code}` | ADMIN |
| `PUT` | `/api/admin/email-templates/{code}` | ADMIN |
| `POST` | `/api/admin/email-templates/{code}/reset` | ADMIN |
| `POST` | `/api/admin/email-templates/{code}/preview` | ADMIN |

Cada cambio se registra en la auditoría. La vista previa usa datos de ejemplo y
el mismo renderizador que el envío real.

## 9. Configuración

```properties
app.mail.enabled=${MAIL_ENABLED:false}
app.mail.from=${MAIL_FROM:no-reply@pardrops.local}
app.mail.from-name=${MAIL_FROM_NAME:ParDrops}
app.mail.outbox-dir=./data/mail-outbox
app.mail.frontend-url=${APP_FRONTEND_URL:http://localhost:4200}

spring.mail.host=${MAIL_HOST:localhost}
spring.mail.port=${MAIL_PORT:1025}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=${MAIL_SMTP_AUTH:false}
spring.mail.properties.mail.smtp.starttls.enable=${MAIL_STARTTLS:false}
```

### Gmail

1. Activa la verificación en dos pasos en la cuenta.
2. Crea una **contraseña de aplicación**.
3. Configura:

```properties
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-correo@gmail.com
MAIL_PASSWORD=la-contraseña-de-aplicacion
MAIL_SMTP_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=tu-correo@gmail.com
```

Nunca se guarda la contraseña en el repositorio: va en `.env.local`.

### Mailpit en local

```bash
mailpit
```

Interfaz en `http://localhost:8025` y SMTP en `localhost:1025`. Con
`MAIL_ENABLED=true`, `MAIL_HOST=localhost`, `MAIL_PORT=1025`.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| No hay pago real | Comprobante ligado a `PAID` y texto explícito |
| SMTP caído | `try/catch`; el pedido continúa |
| Correo del vendedor inválido | Validación y respaldo a `User.email` |
| Duplicados | Un correo por evento; `PAID` no genera además el de estado |
| HTML malicioso en datos | Escape de todos los valores del usuario |
| Plantilla borrada o vacía | Validación y restauración predeterminada |
| Credenciales filtradas | Solo variables de entorno y `.env.local` ignorado |

## 11. Plan de implementación

| Issue | Entrega |
|---|---|
| #48 | Modelo, DTOs, seeder y plantillas predeterminadas |
| #49 | SMTP, outbox y renderizador |
| #50 | Correo al vendedor |
| #51 | Comprobante de compra |
| #52 | Correos de cambio de estado |
| #53 | Editor en el panel de administración |
| #54 | Pruebas |
| #55 | Documentación final |
