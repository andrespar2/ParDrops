# Pedidos multivendedor

## Concepto

En un marketplace con múltiples vendedores, un **único pedido** puede contener productos de distintas tiendas. ParDrops modela esto con:

- **Order**: pedido global con datos del cliente, dirección, totales globales e información común.
- **OrderSubOrder**: una **parte del pedido por vendedor/tienda**. Cada parte tiene su propio estado, totales y líneas.
- **OrderItem**: línea individual que pertenece a un `OrderSubOrder` y referencia al vendedor.

## Creación del pedido

`OrderService.create(...)` realiza el siguiente proceso:

1. **Validación y cálculo**: valida disponibilidad/stock, calcula `subtotal`, aplica impuestos (`TAX_RATE`) y envío (`SHIPPING_COST` / `FREE_SHIPPING_THRESHOLD`), establece totales globales.
2. **Agrupación por vendedor**: agrupa las líneas por `product.seller` (clave `sellerId`). Los productos sin vendedor se agrupan bajo la tienda plataforma `ParDrops`.
3. **Creación de subpedidos**: por cada grupo crea `OrderSubOrder` con `storeName`/`storeSlug` obtenidos de `Store` del vendedor (o valores por defecto). Se asigna estado inicial `PENDING`.
4. **Distribución de importes**: reparte impuestos y gastos de envío entre las partes proporcionalmente, ajustando el último redondeo para que la suma coincida con los totales globales.
5. **Notificación**: notifica a cada vendedor con parte de pedido (`"Tienes una venta nueva"`).
6. **Notificación al cliente**: informa sobre la creación del pedido con desglose por tienda.

## Estados y transiciones

Los estados son: `PENDING`, `PAID`, `PREPARED`, `SHIPPED`, `DELIVERED`, `CANCELLED`. `DELIVERED` y `CANCELLED` son terminales.

**Transiciones permitidas** (definidas en `OrderStatus`):

- `PENDING` → `PAID`, `PREPARED`, `SHIPPED`, `CANCELLED`
- `PAID` → `PREPARED`, `SHIPPED`, `CANCELLED`
- `PREPARED` → `SHIPPED`, `CANCELLED`
- `SHIPPED` → `DELIVERED`, `CANCELLED`
- `DELIVERED`, `CANCELLED` → (ninguna)

## Gestión por roles

| Acción | Quién | Endpoint | Notas |
|---|---|---|---|
| Crear pedido | Invitado/Cliente | `POST /api/orders` | Público. Genera pedido global + subpedidos. |
| Ver mis pedidos | Cliente | `GET /api/orders/me` | Incluye `subOrders` con estado por tienda y productos. |
| Ver todos los pedidos | ADMIN | `GET /api/orders` | Panel de administración. |
| Cambiar estado global | ADMIN | `PATCH /api/orders/*/status` | Actualiza estado según transiciones. Puede afectar al pedido global. |
| Ver subpedidos propios | SELLER | `GET /api/seller/orders` | Solo las partes de su tienda. |
| Avanzar estado de subpedido propio | SELLER | `PATCH /api/seller/orders/*/status` | Solo sobre sus propias partes; respeta transiciones. Notifica al cliente. |

## Experiencia de usuario

- **Cliente (`/cuenta`)**: ve cada pedido con sus líneas y, debajo, el **desglose por tienda** mostrando estado, tienda (enlace a `/tiendas/:slug`), importes y líneas de esa parte.
- **Vendedor (`/vendedor/pedidos`)**: ve únicamente sus subpedidos con datos del cliente, dirección de envío, líneas, totales y botones para avanzar estado. No puede ver pedidos completos de otras tiendas.
- **Producto/ficha**: se indica la tienda/vendedor desde la tarjeta de producto y desde la ficha de detalle, enlazando a la tienda pública.

## Notificaciones relacionadas

- **Nueva venta**: al vendedor propietario del `OrderSubOrder` cuando se crea el pedido.
- **Avance de estado**: al cliente cuando el vendedor actualiza el estado de su parte (`"Tu pedido va avanzando"` con nombre de tienda y nuevo estado).
- **Pedido creado**: al cliente con resumen de la compra.
