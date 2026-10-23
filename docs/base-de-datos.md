# Base de datos

## Configuración

- **Motor**: H2 (archivo)
- **URL**: `jdbc:h2:file:./data/pardropsdb;DB_CLOSE_ON_EXIT=FALSE`
- **DDL**: `spring.jpa.hibernate.ddl-auto=update` (crea/actualiza esquema automáticamente)
- **Consola H2**: `http://localhost:8080/h2-console` (habilitada en desarrollo)
- **Datos**: carpeta `./data/` excluida del repositorio (`.gitignore`)

## Entidades principales del marketplace

| Entidad | Tabla | Descripción |
|---|---|---|
| `User` | `users` | Usuarios con `role` (`CUSTOMER`/`SELLER`/`ADMIN`), estado, credenciales. |
| `Store` | `stores` | Tienda de un vendedor. Relación `@ManyToOne` a `User` (propietario). Campos `name`, `slug` único, contacto, imágenes, `status`. |
| `SellerApplication` | `seller_applications` | Solicitud para vender. Estado (`PENDING`/`APPROVED`/`REJECTED`), notas de revisión, revisor. |
| `Product` | `products` | Producto del catálogo. Pertenece a `User` (`seller`). Incluye `active`, `sellerId`, `sellerName`, `storeSlug` (transientes/derivados). Imágenes/variantes/categorías/tags como `@ElementCollection`. |
| `ProductImage`, `ProductVariant` | Embebidos/element collections | Estructuras de producto. |
| `Category` | `categories` | Categorías administrables. |
| `Order` | `orders` | Pedido global. Datos de cliente/envío, totales (`subtotal`, `tax`, `shipping`, `total`), `status`, moneda. Relación `@OneToMany` con `OrderItem` y `OrderSubOrder`. |
| `OrderSubOrder` | `order_sub_orders` | Parte del pedido por tienda/vendedor. `seller`, `storeName`, `storeSlug`, totales, `status`, `createdAt`. Relación con `OrderItem`. |
| `OrderItem` | `order_items` | Línea de pedido. Referencia a `Order`, `OrderSubOrder`, `Product`/`User` vendedor, cantidad, precios. |
| `ProductReview` | `product_reviews` | Reseñas con restricción de elegibilidad (solo quien compró y recibió). |
| `Notification` | `notifications` | Avisos por usuario con `type`, lectura, enlace. |
| `MediaAsset` | `media_assets` | Metadatos de imagen subida (`originalName`, `storedName`, `publicPath`, `mimeType`, `sizeBytes`, propietario). |
| `AuditLog` | `audit_logs` | Auditoría de acciones administrativas (`AuditAction`). |

## Migraciones/configuración automática

- **Migraciones transaccionales** (`@Component` con `ApplicationRunner`/`@PostConstruct`):
  - `MarketplaceSchemaMigration`: normaliza valores de `users.role` a `CUSTOMER`/`SELLER`/`ADMIN`.
  - `OrderStatusSchemaMigration`: normaliza valores de `orders.status` a los estados válidos.
  - `CurrencyMigration`: normaliza `orders.currency` a `COP`.
- **DataSeeder**: carga datos iniciales en desarrollo (incluidas cuentas demo y catálogo base).

## Integridad y consideraciones

- **Relaciones lazy**: uso de `FetchType.LAZY` para evitar cargas innecesarias; `@JsonIgnore`/serialización controlada.
- **Slug único**: `Store.slug` debe ser único; validado en backend y frontend.
- **Aislamiento**: las consultas del panel vendedor filtran siempre por `sellerId`.
- **Estados de pedido**: transición controlada por lógica de negocio (ver `OrderStatus`).
- **Persistencia de imágenes**: metadatos en BD, ficheros en disco fuera de la BD.
