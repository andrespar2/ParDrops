# Comisiones, tienda, variantes y reseñas

Diseño de las funcionalidades de comisión, saldo retenido, perfil público de
tienda, variantes, reseñas con imágenes y rankings de portada.

## 1. Comisión y saldo

### Regla

- Comisión: **5% del importe de productos** (`OrderSubOrder.subtotal`).
- No se cobra comisión sobre IVA ni sobre envío.
- El vendedor recibe `OrderSubOrder.total - comisión`.
- La tasa se guarda en cada subpedido para que un cambio futuro no altere
  ventas pasadas.

Ejemplo:

```text
Subtotal productos: 100.000
IVA:                 19.000
Envío:               25.900
Total cobrado:      144.900
Comisión 5%:          5.000
Neto vendedor:      139.900
```

### Ciclo de los fondos

```text
Pedido creado
  └─ fondos HELD
       ├─ saldo pendiente del vendedor += neto
       └─ comisión calculada (aún no disponible)

Pedido entregado
  └─ fondos RELEASED
       ├─ saldo pendiente -= neto
       ├─ saldo disponible += neto
       └─ comisión acumulada += comisión

Pedido cancelado antes de entregar
  └─ fondos CANCELLED
       └─ saldo pendiente -= neto
```

- La liberación es idempotente: si el subpedido ya está `RELEASED`, no se
  vuelve a abonar.
- El administrador puede marcar el pedido global como `DELIVERED`; en ese caso
  se liberan todas las partes no terminales con vendedor.
- Los subpedidos de plataforma (`seller == null`) no tienen saldo de vendedor.

### Modelo

`OrderSubOrder`:

| Campo | Uso |
|---|---|
| `commissionRate` | Tasa aplicada (0.05) |
| `commissionAmount` | Comisión en COP |
| `sellerNetAmount` | Neto para el vendedor |
| `fundsStatus` | `HELD`, `RELEASED`, `CANCELLED` |
| `releasedAt` | Fecha de liberación |

`SellerBalance`:

| Campo | Uso |
|---|---|
| `pendingAmount` | Neto retenido |
| `availableAmount` | Neto disponible |
| `commissionTotal` | Comisión acumulada |
| `totalSales` | Subtotal de productos vendidos |
| `updatedAt` | Última actualización |

### Datos históricos

Una migración rellena los subpedidos antiguos y reconstruye los saldos una sola
vez. Si la tabla de saldos ya tiene datos, no se vuelve a reconstruir.

## 2. Tienda pública y privacidad

### Datos públicos

- Nombre de la tienda.
- Slug.
- Descripción.
- Logo y portada.
- Correo y teléfono **públicos de la tienda**.
- Ciudad.
- Rating y número de reseñas.
- Catálogo publicado.

### Datos privados

- Nombre legal.
- Tipo y número de documento.
- Dirección legal.
- Teléfono personal.
- Correo de notificaciones.
- Datos fiscales.

Solo los ve el administrador y el propio vendedor en su ficha privada.

### Cambios

- `Store` gana `publicEmail`, `publicPhone`, `city` y `notificationEmail`.
- `SellerApplication` gana los datos legales y los públicos de tienda.
- Al aprobar, solo se copian los datos públicos a `Store`.
- `StoreResponse` deja de exponer `sellerName`, el correo personal y los datos
  fiscales.
- Los productos muestran el nombre de la tienda, no el nombre personal.
- Se oculta `Product.seller` y `User.password` en el JSON.

## 3. Perfil público

`GET /api/stores/{slug}` devuelve la ficha con `rating` y `reviewCount`
calculados desde las reseñas de los productos del vendedor.

`GET /api/stores/{slug}/reviews` devuelve las reseñas recientes con autor,
estrellas, comentario e imágenes.

La página `/tiendas/:slug` muestra:

- Portada, logo, nombre y descripción.
- Estrellas y número de reseñas.
- Contacto público.
- Reseñas.
- Catálogo de la tienda.

Una tienda suspendida devuelve 404.

## 4. Variantes

### Modelo

`ProductVariant` añade:

| Campo | Uso |
|---|---|
| `size` | Talla (opcional) |
| `color` | Color (opcional) |
| `imageUrl` | Imagen de la versión |
| `name` | Etiqueta visible |
| `price` / `stock` / `sku` | Precio, stock y referencia |

`OrderItem` guarda `variantId` y `variantSku` además de `variantName`.

### Comportamiento

- Editar un producto sin enviar `variants` conserva las existentes.
- Enviar una lista vacía las elimina.
- El precio y el stock se resuelven por `variantId`, con respaldo por nombre.
- Se descuenta el stock de la variante y el del producto.
- No se puede comprar una variante agotada.
- La ficha muestra selectores de talla y color y la lista de versiones.
- La tarjeta muestra “Desde” si los precios difieren.

## 5. Reseñas con imágenes

- Máximo 3 imágenes por reseña.
- Se suben con el sistema de medios existente y se guardan como URLs.
- Se muestran en la ficha del producto y en el perfil de la tienda.
- El administrador puede listar y eliminar reseñas desde `/admin/resenas`.
- Al eliminar se recalculan `rating` y `reviewCount` y se registra
  `REVIEW_DELETED` en auditoría.
- Un cliente no puede acceder a la moderación.

## 6. Portada

`GET /api/products/highlights`:

- `bestSellers`: productos con más unidades vendidas en pedidos no cancelados.
- `topRated`: productos con mejor media de estrellas y al menos una reseña.

La portada muestra dos secciones con las tarjetas existentes y un enlace al
catálogo.

## 7. Riesgos

| Riesgo | Mitigación |
|---|---|
| Doble liberación de saldo | `fundsStatus` idempotente |
| Comisión sobre IVA o envío | Base `subtotal` documentada |
| Fuga de datos personales | DTOs públicos separados y pruebas de serialización |
| Variantes borradas al editar | Conservar si `variants == null` |
| Sobreventa de variantes | Validar y descontar stock de la variante |
| Imágenes huérfanas | Límite y validación; el borrado de reseña no elimina archivos compartidos |
| Media sin reseñas | Recalcular rating al borrar |
| Productos ocultos en portada | Filtrar por `active` y stock |

## 8. Plan

| Etapa | Issues |
|---|---|
| Diseño | #57 |
| Comisiones y saldo | #58, #59, #60 |
| Tienda y perfil | #61, #62 |
| Variantes | #63, #64 |
| Reseñas | #65 |
| Portada | #66 |
| Pruebas y documentación | #67, #68 |
