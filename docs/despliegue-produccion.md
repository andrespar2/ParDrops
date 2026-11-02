# Despliegue a producción: decisiones y registro

Documento de apoyo de [`DEPLOYMENT.md`](../DEPLOYMENT.md). Recoge la auditoría
inicial, las decisiones técnicas y el resultado de la migración de datos.

---

## 1. Auditoría del repositorio

| Elemento | Hallazgo |
|---|---|
| Monorepo | `pardrops/` (backend) y `pardropsfrontend/` (frontend) |
| Backend | Java 25, Spring Boot 4.1.1, Maven Wrapper, Lombok |
| Frontend | Angular 22, TypeScript 6, SSR configurado con todas las rutas en modo cliente |
| Base de datos local | H2 en archivo (`pardrops/data/pardropsdb.mv.db`), 17 tablas |
| Seguridad | JWT propio, roles ADMIN/SELLER/CUSTOMER, Google OAuth2 opcional |
| Multimedia | `MediaStorage` con implementación en sistema de archivos |
| Correo | SMTP opcional con outbox local y plantillas editables |
| CI/CD | No existía ningún workflow |
| Despliegue previo | Worker `pardrops` en Cloudflare creado desde plantilla (`has_assets: false`), sin proyecto de Pages ni rutas |

Riesgos detectados:

1. **SQL específico de H2** en `*SchemaMigration`: `ALTER TABLE ... ALTER COLUMN
   ... ENUM(...)`. Estaba protegido por una comprobación de tipo (`DATA_TYPE =
   'ENUM'`), así que no se ejecuta en PostgreSQL.
2. **Tablas de unión sin clave primaria** (`product_categories`,
   `product_tags`, `product_images`, `product_variants`): al copiar datos pueden
   duplicarse. Se deduplicaron y se añadieron índices únicos donde corresponde.
3. **Disco efímero en Render**: las imágenes subidas se perderían en cada
   despliegue. Se resolvió guardándolas en PostgreSQL.
4. **`localhost` en el frontend**: `api.config.ts` tenía la URL fija. Se movió a
   configuración de ejecución.

---

## 2. Decisiones técnicas

### 2.1 PostgreSQL sin Flyway

El proyecto no tenía migraciones versionadas: el esquema lo crea Hibernate con
`ddl-auto=update` y las clases `*SchemaMigration` ajustan datos de bases
preexistentes de forma idempotente. Introducir Flyway sobre una base ya poblada
obligaría a un *baseline* generado a mano y a reescribir esas migraciones, con
riesgo alto y beneficio nulo para esta entrega. Se mantiene el mecanismo actual
y se documenta como deuda técnica.

### 2.2 Perfil `prod` y variables

`application.properties` queda con valores de desarrollo y `application-prod.properties`
con los de producción (consola H2 apagada, pool ajustado, compresión, cookies
seguras, health check). Ningún secreto vive en el repositorio.

`DatabaseUrlSupport` permite usar `DATABASE_URL` (formato Neon) además de
`SPRING_DATASOURCE_URL`, y añade `prepareThreshold=0` cuando se usa el pooler.

### 2.3 Imágenes en PostgreSQL

`MediaStorage` ya era una interfaz, así que se añadió `DatabaseMediaStorage`
(perfil `prod`) con una entidad `media_files` (`bytea`) y
`MediaFileController` sirviendo `GET /media/{nombre}` con caché de 30 días.
En local se sigue usando el sistema de archivos (`@Profile("!prod")`).

### 2.4 Frontend: SPA en lugar de SSR

Todas las rutas usan `RenderMode.Client`, por lo que el servidor SSR no aportaba
nada y Cloudflare Pages sirve estáticos. El build por defecto genera solo el
bundle del navegador; la configuración `ssr` sigue disponible con
`npm run build:ssr`.

La URL de la API se resuelve en tiempo de ejecución desde `public/config.js`,
que el pipeline reescribe con `PARDROPS_API_URL`. Así el mismo artefacto
compilado funciona en local y en producción.

### 2.5 Cloudflare Pages frente a Workers con Assets

Pages ofrece despliegue por CLI sin configuración, dominios personalizados y
certificados automáticos, y cubre de sobra un sitio estático. Workers con
Static Assets habría requerido un `wrangler.jsonc` y no aportaba ventajas para
este caso.

---

## 3. Migración de datos

| Tabla | Filas H2 | Filas PostgreSQL |
|---|---|---|
| users | 27 | 27 |
| products | 10 | 10 |
| categories | 12 | 12 |
| orders | 7 | 7 |
| order_items | 10 | 10 |
| order_sub_orders | 5 | 5 |
| stores | 7 | 7 |
| seller_applications | 6 | 6 |
| audit_logs | 34 | 34 |
| notifications | 34 | 34 |
| email_templates | 3 | 3 |
| product_categories | 19 | 19 |
| product_images | 10 | 10 |
| product_tags | 28 | 28 |
| product_reviews | 0 | 0 |
| product_variants | 0 | 0 |
| media_assets | 0 | 0 |

- Copia de seguridad: `backups/h2-<timestamp>/pardropsdb.mv.db`, SHA-256
  `116dd453d0983257487e0e7478979ceae7e51c03416eabf6ecb9b6ec240a8f4c`, idéntico
  al original. La base H2 no se modificó.
- Secuencias `id` sincronizadas con `setval` al máximo real de cada tabla.
- Deduplicación: `product_categories` 31→19, `product_images` 17→10,
  `product_tags` 46→28.

---

## 4. Verificación funcional

Ejecutada contra la API con PostgreSQL real (12/12 comprobaciones):

| Comprobación | Resultado |
|---|---|
| Registro de usuario | 201 |
| Login y emisión de JWT | 200 |
| Perfil autenticado (`/api/auth/me`) | 200 |
| Ruta protegida sin token | 401 |
| Catálogo público | 200, 10 productos |
| Creación de pedido autenticado | 201 |
| Pedido recuperable en `/api/orders/me` | 1 pedido |
| Subida de imagen | 201 |
| Imagen servida desde PostgreSQL | 200, bytes idénticos |
| Panel de administración | 200 |
| Usuario normal en ruta de admin | 403 |

Pruebas automáticas del repositorio: backend **235 tests, 0 fallos**; frontend
compilado sin errores (solo avisos de presupuesto de estilos preexistentes).

---

## 5. Pendientes conocidos

1. **DNS del dominio**: `pardrops.qd.je` es una zona delegada cuyo apex no puede
   ser CNAME en DigitalPlat; requiere un proveedor con alias en el apex. Ver
   `DEPLOYMENT.md` §7.
2. **Google OAuth2 en producción**: hay que registrar en Google Cloud Console el
   nuevo `redirect_uri` (`https://pardrops-api.onrender.com/login/oauth2/code/google`).
3. **Contraseña del administrador heredado**: la cuenta `admin@pardrops.com`
   conserva la contraseña de desarrollo. Debe cambiarse.
4. **Rotación de credenciales**: los tokens de Cloudflare, Render, Neon y GitHub
   usados en esta entrega deben rotarse tras el despliegue.
5. **Worker `pardrops`**: resto del despliegue fallido anterior, sin rutas; puede
   eliminarse manualmente.
