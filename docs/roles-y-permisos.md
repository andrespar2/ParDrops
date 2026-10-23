# Roles y permisos

## Roles disponibles

| Rol | Descripción | Alcance |
|---|---|---|
| `CUSTOMER` | Cliente registrado o usuario base. | Compra, historial propio, reseñas elegibles, solicitar convertirse en vendedor. |
| `SELLER` | Vendedor con tienda activa. | Panel privado `/vendedor`, gestión de sus productos y subpedidos, edición de su ficha comercial. |
| `ADMIN` | Administrador de la plataforma. | Gestión completa: productos globales, pedidos, usuarios, categorías, solicitudes de vendedor, auditoría, panel admin. |

## Permisos por funcionalidad

| Funcionalidad | Invitado | CUSTOMER | SELLER | ADMIN |
|---|:---:|:---:|:---:|:---:|
| Explorar catálogo (`/api/products`, `/api/products/{slug}`) | ✅ | ✅ | ✅ | ✅ |
| Ver tienda pública (`/api/stores`, `/api/stores/{slug}`, `/api/stores/{slug}/products`) | ✅ | ✅ | ✅ | ✅ |
| Añadir al carrito / ver carrito | ✅ | ✅ | ✅ | ✅ |
| Crear pedido (`POST /api/orders`) | ✅ | ✅ | ✅ | ✅ |
| Ver mis pedidos (`GET /api/orders/me`) | ❌ | ✅ | ✅ | ✅ |
| Ver estado por tienda en mi cuenta | ❌ | ✅ | ✅ | ✅ |
| Publicar reseña (si elegible) (`POST /api/products/*/reviews`) | ❌ | ✅ | ✅ | ✅ |
| Solicitar ser vendedor (`POST /api/seller-applications`) | ❌ | ✅ | ✅ | ✅ |
| Consultar mi solicitud (`GET /api/seller-applications/me`) | ❌ | ✅ | ✅ | ✅ |
| Panel vendedor (`/vendedor/*`, `/api/seller/**`) | ❌ | ❌ | ✅ | ❌ |
| Gestionar productos propios (`/api/seller/products`) | ❌ | ❌ | ✅ | ❌ |
| Gestionar subpedidos propios (`/api/seller/orders`) | ❌ | ❌ | ✅ | ❌ |
| Ver/editar ficha propia (`GET/PUT /api/stores/mine`) | ❌ | ❌ | ✅ | ❌ |
| Panel administración (`/api/admin/**`) | ❌ | ❌ | ❌ | ✅ |
| Revisar solicitudes de vendedor (`GET/PATCH /api/seller-applications`) | ❌ | ❌ | ❌ | ✅ |
| Cambiar estado global de pedidos (`PATCH /api/orders/*/status`) | ❌ | ❌ | ❌ | ✅ |
| Subir/gestionar imágenes propias (`/api/media/**`) | ❌ | ✅ | ✅ | ✅ |
| Notificaciones propias (`/api/notifications/**`) | ❌ | ✅ | ✅ | ✅ |

## Reglas de seguridad aplicadas

- **JWT stateless**: todas las rutas protegidas exigen token válido.
- **Guards frontend**: `sellerGuard` protege `/vendedor/*`; `adminGuard` protege `/admin/*`; `authGuard` protege rutas autenticadas.
- **Aislamiento backend**: los controladores del vendedor filtran por el usuario autenticado. Nunca se acepta un `sellerId` arbitrario desde el cliente.
- **Visibilidad**: un producto con `active=false` solo es visible para su propietario (vendedor) en su panel; no aparece en el catálogo público ni en la tienda pública.
- **Solicitudes**: solo usuarios con rol `CUSTOMER` pueden enviar una nueva solicitud (con reglas de reintento tras rechazo configurables). Al aprobar, se promociona a `SELLER` y se crea una tienda activa.
- **403/401**: el backend responde con JSON estructurado vía `RestAuthenticationEntryPoint` y `RestAccessDeniedHandler`.
