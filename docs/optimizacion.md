# Optimización integral de ParDrops

Fecha de ejecución: 2026-11-04/05  
Alcance: backend Spring Boot, base de datos PostgreSQL/H2, caché Redis opcional,
caché local del navegador, imágenes/CDN, panel administrativo, panel de
vendedor, ranking dinámico y validación.

> Este documento distingue explícitamente entre lo **verificado en ejecución**,
> lo **implementado pero no activado** por falta de credenciales y lo que
> **no se pudo medir**. No se inventan porcentajes de mejora.

## 1. Arquitectura encontrada

| Capa | Tecnología | Versión / detalle |
|---|---|---|
| Backend | Spring Boot (Web MVC, Security, Data JPA, Actuator) | 4.1.1 |
| Lenguaje | Java | 25 |
| Base local | H2 en archivo | `./data/pardropsdb` |
| Base producción | PostgreSQL en Neon | PostgreSQL 18, región `aws-sa-east-1` |
| Frontend | Angular standalone + SSR opcional | 22.x |
| Autenticación | JWT stateless + OAuth2 Google opcional | — |
| Correo | SMTP o API HTTPS de Brevo | — |
| Pagos | Wompi sandbox/producción | — |
| Frontend productivo | Cloudflare Pages (`pardrops`) | Dominios `pardrops.pages.dev`, `www.pardrops.qd.je` |
| Backend productivo | Render `pardrops-api` | Plan free |
| Despliegue | GitHub Actions + Render API + Wrangler | — |

### Base de datos

Entidades principales: `users`, `stores`, `products`, `product_images`,
`product_categories`, `product_tags`, `product_variants`, `product_reviews`,
`product_review_images`, `orders`, `order_items`, `order_sub_orders`,
`notifications`, `media_assets`, `media_files`, `seller_applications`,
`audit_logs`, `categories`.

El esquema se gestiona con `spring.jpa.hibernate.ddl-auto=update` y migraciones
idempotentes al arrancar. No existe Flyway/Liquibase, por lo que las nuevas
migraciones siguen el patrón existente de `ApplicationRunner`.

### Problemas detectados en la auditoría

1. El catálogo público descargaba **todos** los productos publicados.
2. El panel administrativo podía listar todo el catálogo sin paginar cuando no
   se enviaban filtros.
3. `ProductService.enrich` cargaba todas las tiendas y podía generar consultas
   repetidas; `toAdminResponse` resolvía la tienda producto a producto (N+1).
4. `StoreService.findAllActive` ejecutaba dos consultas de reseñas por tienda.
5. `SellerDashboardService` traía todos los subpedidos para contar y sumar.
6. `AdminDashboardService` cargaba todos los productos publicados solo para
   contarlos.
7. No había índices para filtros por vendedor, categoría, estado, fecha,
   calificaciones ni para el historial de pedidos.
8. En producción las imágenes se guardaban como BLOB en PostgreSQL.
9. No existía caché de servidor ni caché local acotada.
10. El panel de vendedor no tenía consulta de reseñas y su navegación no era
    homogénea con la del administrador.
11. El panel administrativo no organizaba el catálogo por tiendas.
12. El orden del catálogo era estático (fecha de creación).
13. El manejo de errores devolvía el mensaje interno en los 500.

## 2. Cambios implementados

### 2.1 Redis opcional (cache-aside)

- Dependencia `spring-boot-starter-data-redis` añadida.
- `CacheService` con dos implementaciones:
  - `NoOpCacheService` cuando `APP_REDIS_ENABLED=false` (por defecto).
  - `RedisCacheService` cuando `APP_REDIS_ENABLED=true`.
- Claves con prefijo `pardrops:` y versión (`v1`), por entidad:
  `productos:publicados`, `productos:feed`, `productos:ranking`,
  `productos:vendedor`, `productos:tienda`, `productos:detalle`,
  `tiendas:publicas`, `tiendas:resumen`, `resenas:producto`,
  `resenas:vendedor`, `categorias`.
- TTL configurables por variable de entorno.
- Serialización JSON con el `ObjectMapper` de Spring Boot 4 (Jackson 3).
- Protección ante fallos: periodo de enfriamiento de 30 s; si Redis falla, la
  petición continúa contra la base de datos.
- Invalidación **después del commit** con `CacheInvalidator`.
- Métricas en `GET /api/admin/cache/stats` (aciertos, fallos, escrituras,
  invalidaciones, errores, ratio y disponibilidad).
- Sin Redis no se activa el health indicator de Redis ni se escanean
  repositorios Redis.

**Estado verificado:** arranque y funcionamiento completo sin Redis.  
**No verificado en vivo:** Redis real, porque no hay instancia disponible en el
equipo, Docker no está instalado y WSL no tiene `redis-server`. El adaptador
está aislado y no se declara probado contra un servidor real.

### 2.2 Caché local del navegador

- `LocalCacheService`: `localStorage` con prefijo versionado, TTL, limpieza de
  entradas caducadas y límite global de 2 MB con desalojo de las más antiguas.
- Solo se cachean datos públicos (ficha de producto, rankings). No se guardan
  datos privados de otros usuarios.
- Invalidación al crear/editar/eliminar productos y al publicar reseñas.
- `ProductService` conserva la semilla de distribución en memoria: una recarga
  completa crea una semilla nueva; la navegación interna conserva el orden.
- ETag HTTP en endpoints públicos de catálogo/tiendas mediante
  `ShallowEtagHeaderFilter`.

### 2.3 Ranking dinámico del catálogo

Endpoint nuevo: `GET /api/products/feed`.

Parámetros: `seed`, `page`, `size`, `search`, `category`, `minPrice`,
`maxPrice`, `new`, `sale`, `sort`.

Fórmula (pesos iniciales configurables):

```
score = 0.38 * calidad_bayesiana
      + 0.18 * novedad
      + 0.24 * popularidad_por_compras
      + 0.12 * afinidad_categorias_compradas
      + 0.08 * disponibilidad
      + jitter_determinista(seed, producto) * 0.10
```

- Calidad: media bayesiana con prior 4.0 y peso 5 para evitar que una sola
  reseña de 5 estrellas supere a muchas reseñas positivas.
- Novedad: decaimiento exponencial con vida media de 30 días.
- Popularidad: `log1p(unidades vendidas)` normalizado; no cuenta visitas.
- Afinidad: categorías de productos ya comprados por el usuario autenticado.
  Invitados reciben recomendación general.
- Jitter: hash con avalancha sobre `seed + id`; no es aleatoriedad completa.
- La semilla se reenvía en cada página; no se duplican ni omiten productos.
- Los agregados se cachean en memoria y Redis; nunca se guarda una distribución
  personalizada como orden global.
- `ORDER BY RANDOM()`/`RAND()` no se usan.

**Verificado:** dos semillas producen órdenes distintos con el mismo conjunto de
productos; páginas consecutivas no se solapan; el orden se mantiene con la misma
semilla.

### 2.4 Base de datos e índices

Migración idempotente `DatabaseIndexMigration` compatible con H2 y PostgreSQL.
Crea 26 índices justificados (no toca índices únicos existentes):

- `products(active, seller_id, created_at)`
- `products(seller_id, active)`
- `products(active, created_at)`
- `products(active, price)`
- `products(active, rating, review_count)`
- `product_images(product_id)`
- `product_categories(product_id)`
- `product_categories(category, product_id)`
- `product_tags(tag)`
- `product_variants(product_id)`
- `product_reviews(product_id, created_at)`
- `product_reviews(user_id)`
- `orders(user_id, created_at)`
- `orders(status, created_at)`
- `order_items(order_id)`
- `order_items(product_id)`
- `order_items(sub_order_id)`
- `order_sub_orders(seller_id, created_at)`
- `order_sub_orders(order_id)`
- `notifications(user_id, is_read, created_at)`
- `media_assets(owner_id, created_at)`
- `seller_applications(status, created_at)`
- `audit_logs(created_at)`
- `users(role, status)`
- `categories(active, featured)`
- `stores(status, name)`

Consultas optimizadas:

- Proyección ligera para ranking (solo escalares, sin imágenes/variantes).
- Agregados por vendedor para contadores de tienda y valoraciones.
- `countPublished()` en lugar de cargar todos los productos publicados.
- Contadores y sumas de subpedidos en SQL en lugar de listas en memoria.
- `enrich` carga las tiendas de los vendedores en una sola consulta y evita N+1.

### 2.5 Imágenes y CDN

- `R2MediaStorage` opcional (`APP_MEDIA_STORAGE=r2`) compatible con la API S3 de
  Cloudflare R2, firmando peticiones SigV4 con `java.net.http`.
- Guarda solo referencias y metadatos en `media_assets`; los bytes van al bucket.
- `Cache-Control: public, max-age=31536000, immutable` en objetos y en `/media`.
- Nombres UUID versionados; ETag en la respuesta de `/media`.
- `MediaDatabaseToR2Migration` copia BLOB históricos a R2 y actualiza
  referencias en productos, variantes, tiendas y reseñas **sin borrar** los
  originales de `media_files`.
- `MediaController` respeta URLs absolutas de CDN (no les antepone el host de la
  API).
- `public/_headers` de Cloudflare Pages cachea chunks JS/CSS con hash durante un
  año y evita cachear `index.html`/`config.js`.

**Estado:** implementado y compilado, pero **no activado** porque el API Token
de Cloudflare no tiene permisos de R2 y no hay claves S3 de R2 disponibles. La
migración no se ejecutó. El almacenamiento actual sigue funcionando sin cambios.

### 2.6 Panel administrativo: productos por tienda

- Nuevo endpoint `GET /api/admin/product-stores` con contadores agregados:
  total, publicados y ocultos por tienda, más el catálogo propio ParDrops.
- La sección de productos ahora muestra primero tiendas paginadas con buscador.
- Al seleccionar una tienda se consulta solo su catálogo (`sellerId`), con
  búsqueda, filtro de visibilidad y paginación.
- El catálogo propio se consulta con `sellerId=-1` (sin vendedor).
- La edición carga el producto completo con
  `GET /api/admin/products/{id}/full`, incluso si está oculto.
- Imágenes de tabla con `loading="lazy"`.

### 2.7 Panel de vendedor

- Navegación con iconos y estructura equivalente al panel administrativo.
- Nueva sección `Reseñas` en `/vendedor/resenas`.
- Endpoint de solo lectura `GET /api/seller/reviews`, filtrado por el vendedor
  autenticado.
- No existe `POST`, `PUT`, `PATCH` ni `DELETE` de reseñas para SELLER.
- Un `POST` directo del vendedor responde **405**; el acceso al panel admin
  responde **403**.
- El administrador conserva la moderación (`DELETE /api/admin/reviews/{id}`).

### 2.8 Seguridad y consistencia

- `.gitignore` ampliado para `.env*`, claves, certificados y credenciales.
- Los errores 500 ya no devuelven el mensaje interno ni detalles de SQL.
- Respuesta 405 explícita para métodos no permitidos.
- Invalidación de caché solo tras confirmar la escritura.
- Redis y la caché local son aceleradores; la base de datos sigue siendo la
  fuente de verdad.

## 3. Cloudflare: hallazgos reales

Consultas realizadas con el token del archivo de credenciales, sin imprimir sus
valores:

- Token activo (`/user/tokens/verify` → `active`).
- Cuenta accesible: `Pardrops1508@gmail.com's Account`.
- Proyecto Pages: `pardrops`, dominios `pardrops.pages.dev` y
  `www.pardrops.qd.je`.
- Zonas visibles con este token: **0**.
- R2: **HTTP 403** (el token no tiene permiso de administración de R2).

Por tanto:

- Se optimizó la caché de estáticos con `_headers` de Pages.
- No se pudo inspeccionar ni modificar DNS, reglas de caché de zona, WAF ni
  purgas de CDN.
- No se pudo crear ni configurar un bucket R2.
- No se cambió ningún DNS ni recurso de producción.

### Credenciales que faltan para completar CDN/R2

1. Un API Token de Cloudflare con permiso `Zone:Read` y `Cache Purge` para el
   dominio, o acceso al panel de la zona.
2. Credenciales S3 de R2: `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`,
   `R2_SECRET_ACCESS_KEY`, `R2_PUBLIC_BASE_URL`.
3. Opcional: una instancia Redis (Render Key Value, Upstash, etc.) si se quiere
   activar la caché de servidor en producción.

## 4. Pruebas ejecutadas

### Backend

```
./mvnw -B test
Tests run: 253, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Incluye pruebas nuevas de:

- ranking con semilla estable, variación controlada y orden explícito;
- contadores por tienda y paginación del panel administrativo;
- compatibilidad de servicios existentes.

### Frontend

```
npm run build        → OK (solo warning de presupuesto SCSS en product-detail)
npx ng test --watch=false
Test Files: 21 passed
Tests: 99 passed
```

### Pruebas de humo con backend real (H2 en memoria)

- `/actuator/health` → UP sin Redis.
- `/api/products/feed` con dos semillas → órdenes distintos, mismo conjunto.
- Paginación `page=0` y `page=1` → 0 productos solapados, total correcto.
- `/api/products/highlights` → ETag presente.
- Login admin → `/api/admin/product-stores` devuelve tiendas y contadores.
- Login vendedor → `/api/seller/reviews` responde 200.
- Vendedor → `/api/admin/product-stores` responde 403.
- Vendedor → `POST /api/seller/reviews` responde 405.

No se pudieron medir tiempos “antes” porque el proyecto no tenía métricas ni
instrumentación previas. Los contadores de caché y los endpoints agregados
permiten medir “después” en producción.

## 5. Configuración externa pendiente

| Variable | Estado | Efecto |
|---|---|---|
| `APP_REDIS_ENABLED=true` + host/puerto | Pendiente | Activa Redis; sin ella todo funciona. |
| `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_PUBLIC_BASE_URL` | Pendiente | Activa almacenamiento externo. |
| `APP_MEDIA_STORAGE=r2` | Pendiente | Selecciona R2 en lugar de disco/PostgreSQL. |
| `APP_MEDIA_MIGRATE_TO_R2=true` | Pendiente | Copia BLOB históricos; no borra originales. |
| Token Cloudflare con Zone:Read/Cache Purge | Pendiente | Permite revisar/purgar caché de zona. |
| `RANKING_*` | Opcional | Ajusta pesos del ranking sin desplegar código. |

## 6. Issues, Project, commits y Pull Request

- Issues creados: #117 (Redis), #118 (caché local), #119 (base de datos),
  #120 (admin por tiendas), #121 (panel vendedor), #122 (ranking),
  #123 (CDN/R2), #124 (pruebas y documentación).
- Project usado: **#7 ParDrops | Optimización de Marketplace y Ventas**.
  Estados actualizados: #117 In Review; #123 Blocked; el resto Done.
- Commits locales en `main`:
  - `89f4ad9` perf(cache): Redis opcional y métricas.
  - `0279678` feat(catalogo): ranking dinámico y paginación.
  - `46d335d` perf(bd): índices y consultas agregadas.
  - `b86273b` feat(admin): catálogo por tiendas.
  - `e7c314b` feat(vendedor): panel homologado y reseñas solo lectura.
  - `41f40fd` feat(cdn): R2 opcional y caché de imágenes.
  - `18fdd52` docs(calidad): documentación, caché local y configuración.
  - `e66aa2e` perf(catalogo): evita la carga duplicada inicial.
  - `097a901` perf(catalogo): consulta categorías del ranking por lotes.
- Pull Request: **#125** `perf: optimizacion integral de infraestructura,
  cache, paneles y catalogo`, desde `perf/optimizacion-integral` hacia `main`.
- El push se hizo a la rama del PR, **no a `main`**, para no disparar el
  despliegue automático de Render/Cloudflare.

## 7. Riesgos y limitaciones

- Redis no se probó contra un servidor real; sí se probó el camino sin Redis.
- La migración de imágenes a R2 no se ejecutó; requiere credenciales S3.
- El jitter cambia el orden de productos con puntuaciones cercanas; los
  productos con una ventaja grande pueden mantenerse arriba, que es el
  comportamiento deseado para no degradar relevancia.
- El ranking se calcula sobre el conjunto filtrado de productos; para catálogos
  muy grandes conviene mover los agregados a una tabla materializada. Los
  agregados actuales ya se cachean y no se recalculan por cada página.
- La caché local está limitada a 2 MB; si el usuario bloquea `localStorage`, la
  aplicación sigue funcionando sin caché local.
- El filtro `sellerId=-1` es un centinela interno para el catálogo propio; no se
  expone a usuarios finales.
- El esquema sigue dependiendo de `ddl-auto=update`; los índices se crean de
  forma idempotente al arrancar.
