# Despliegue de ParDrops en producción

Guía de la arquitectura desplegada, las variables de entorno necesarias y los
procedimientos de operación, copia de seguridad y restauración.

> **No incluyas credenciales en este archivo.** Todos los valores sensibles se
> configuran en el panel de Render (backend), en los secretos de GitHub Actions
> (pipeline) y en el panel de Cloudflare. El repositorio solo contiene nombres de
> variables.

---

## 1. Arquitectura

```text
Navegador
   │  HTTPS
   ▼
Cloudflare Pages  ──(assets estáticos Angular)──►  pardrops.pages.dev
   │  (custom domain: pardrops.qd.je)
   │  fetch /api/** y /media/**  (config.js → URL absoluta)
   ▼
Render (Docker, plan free)
   │  Spring Boot 4 · Java 25 · perfil prod
   │  JDBC + TLS
   ▼
Neon PostgreSQL 18 (rama production, región aws-sa-east-1)
```

| Componente | Servicio | URL / identificador |
|---|---|---|
| Frontend | Cloudflare Pages (`pardrops`) | https://pardrops.pages.dev |
| Backend | Render (`pardrops-api`, Docker, free) | https://pardrops-api.onrender.com |
| Base de datos | Neon (`green-forest-00959280`) | rama `production`, base `neondb` |
| Dominio | DigitalPlat (`qd.je`) | https://pardrops.qd.je |
| Pipeline | GitHub Actions | `.github/workflows/ci.yml`, `deploy.yml` |

El frontend es una SPA: todas las rutas se renderizan en el cliente, por lo que
en producción se publica solo el bundle del navegador. La configuración SSR se
conserva (`npm run build:ssr`) por si se necesita en el futuro.

---

## 2. Requisitos locales

| Herramienta | Versión usada |
|---|---|
| Node.js / npm | 24 / 11 |
| JDK | 25 (el proyecto incluye uno portable en `.tools/jdk25`) |
| Maven | 3.10 mediante `pardrops/mvnw` |
| Neon CLI | `neonctl` 8.16+ |
| Wrangler | 4 (se ejecuta con `npx`) |

`dev.ps1` carga `.env.local` y arranca backend (8080) y frontend (4200).

---

## 3. Variables de entorno

### 3.1 Backend (Render)

| Variable | Obligatoria | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sí | `prod` |
| `SPRING_DATASOURCE_URL` | sí¹ | `jdbc:postgresql://<host>/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | sí | usuario de Neon (`neondb_owner`) |
| `SPRING_DATASOURCE_PASSWORD` | sí | contraseña de Neon |
| `APP_JWT_SECRET` | sí | clave de firma JWT, mínimo 32 caracteres |
| `APP_CORS_ALLOWED_ORIGINS` | sí | orígenes separados por comas |
| `APP_FRONTEND_URL` | sí | URL pública del frontend |
| `APP_SEED_ENABLED` | no | `false` crea el esquema sin datos (importaciones) |
| `APP_SEED_DEMO_DATA` | no | `true` carga catálogo de ejemplo |
| `APP_SEED_ADMIN_EMAIL` | no | correo del administrador inicial |
| `APP_SEED_ADMIN_PASSWORD` | no | contraseña del administrador inicial |
| `APP_OAUTH2_SECURE_COOKIES` | no | `true` en HTTPS |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | no | acceso con Google |
| `MAIL_*` | no | SMTP transaccional |
| `DB_POOL_SIZE` | no | conexiones Hikari (por defecto 5) |
| `PORT` | — | lo inyecta Render; no definir |

¹ También se acepta `DATABASE_URL` con el formato `postgresql://...`: la
aplicación lo convierte a JDBC al arrancar (`DatabaseUrlSupport`). Añade
`prepareThreshold=0` automáticamente si la URL apunta al pooler de Neon.

### 3.2 Frontend (Cloudflare / GitHub Actions)

| Variable | Descripción |
|---|---|
| `PARDROPS_API_URL` | URL base de la API **con** `/api`. Se escribe en `public/config.js` antes de compilar. |
| `CLOUDFLARE_API_TOKEN` | secreto de Actions (Pages: Edit) |
| `CLOUDFLARE_ACCOUNT_ID` | secreto de Actions |

`public/config.js` se sirve tal cual, sin `Cache-Control` agresivo, de modo que
puede cambiarse la URL de la API sin recompilar.

---

## 4. Neon PostgreSQL

- Organización: `org-cold-cloud-17549785`
- Proyecto: `green-forest-00959280` (`pardrops-production`)
- Rama: `production` (`br-wispy-dream-b6faj6wd`)
- Región: `aws-sa-east-1` · PostgreSQL 18
- Base: `neondb` · Rol: `neondb_owner`

Obtener la cadena de conexión:

```bash
neonctl connection-string --project-id green-forest-00959280 \
  --branch production --database-name neondb --role-name neondb_owner
```

Conexión de prueba:

```bash
psql "postgresql://<usuario>:<clave>@<host>/neondb?sslmode=require" -c "select version();"
```

El esquema lo mantiene Hibernate (`ddl-auto=update`) junto con las migraciones
idempotentes del proyecto (`MarketplaceSchemaMigration`, `GoogleSchemaMigration`,
...). Esas migraciones detectan el tipo de columna antes de tocar nada, así que
son inocuas en PostgreSQL.

### Copia de seguridad

```bash
# Volcado completo en formato personalizado
pg_dump "postgresql://<usuario>:<clave>@<host>/neondb?sslmode=require" \
  --format=custom --no-owner --no-privileges --file=pardrops-$(date +%F).dump
```

Alternativa sin cliente local: Neon guarda un historial de la rama. Crea una
rama de restauración desde la consola (`Branches → Restore`) con la marca de
tiempo deseada.

### Restauración

```bash
# Restauración destructiva: hacerlo sobre una rama nueva, nunca sobre production
createdb -h <host> -U <usuario> pardrops_restore
pg_restore --dbname="postgresql://<usuario>:<clave>@<host>/pardrops_restore?sslmode=require" \
  --no-owner --no-privileges pardrops-YYYY-MM-DD.dump
```

Después, apunta `SPRING_DATASOURCE_URL` a la base restaurada y valida antes de
promocionarla.

---

## 5. Backend en Render

- Servicio: `pardrops-api` (`srv-dbkbllnlk1mc73be8nu0`), plan **free**, región
  `virginia`, runtime Docker, `autoDeploy` desactivado (lo dispara GitHub Actions).
- Dockerfile: `pardrops/Dockerfile` (multi-stage, usuario sin privilegios, JRE 25).
- Contexto de build: `./pardrops`; ruta del Dockerfile: `./pardrops/Dockerfile`
  (Render la resuelve **desde la raíz del repositorio**).
- Health check: `/actuator/health`.

Notas del plan gratuito: la instancia se suspende tras 15 minutos sin tráfico
(el primer acceso puede tardar ~50 s) y el disco es efímero. Por eso las
imágenes subidas se guardan en PostgreSQL (`DatabaseMediaStorage`, perfil
`prod`) y no en el sistema de archivos.

Reinicio manual: `POST /v1/services/{id}/restart`. Logs:

```bash
curl -s "https://api.render.com/v1/logs?ownerId=<owner>&resource=<service>" \
  -H "Authorization: Bearer $RENDER_API_KEY"
```

---

## 6. Frontend en Cloudflare Pages

- Proyecto: `pardrops`, rama de producción `main`, subdominio `pardrops.pages.dev`.
- Build: `npm ci && npm run build:pages` dentro de `pardropsfrontend/`.
- Directorio publicado: `dist/pardropsfrontend/browser`.
- `_redirects` aplica el fallback SPA (`/* /index.html 200`) y `_headers` fija
  caché larga para los assets con hash y `no-cache` para `config.js`.

Publicación manual:

```bash
cd pardropsfrontend
PARDROPS_API_URL=https://pardrops-api.onrender.com/api npm run build:pages
npx wrangler pages deploy dist/pardropsfrontend/browser --project-name=pardrops --branch=main
```

> El Worker `pardrops` que aparece en Cloudflare es un resto del despliegue
> fallido anterior (creado desde plantilla, sin assets ni rutas). No sirve
> tráfico; puede eliminarse desde el panel sin afectar al proyecto de Pages.

---

## 7. DNS de pardrops.qd.je (DigitalPlat)

Estado actual del dominio:

- `qd.je` está delegado en `ns1.digitalplat.org` … `ns4.digitalplat.org`.
- `pardrops.qd.je` es una **zona delegada propia** (`dns1/dns2.digitalplat.org`),
  es decir, el nombre que controlamos es el **apex** de su zona.
- DigitalPlat no ofrece editor de registros: solo guarda los nameservers. Los
  registros A/CNAME deben crearse en el proveedor DNS externo al que se delegue.

Cloudflare Pages permite dominios personalizados fuera de Cloudflare creando un
`CNAME` hacia `<proyecto>.pages.dev`. Como `pardrops.qd.je` es el apex de su
zona, ese CNAME debe resolverse con *flattening* (alias en el apex), algo que no
todos los proveedores soportan.

Pasos recomendados:

1. En el panel de DigitalPlat, apunta los nameservers de `pardrops.qd.je` al
   proveedor DNS que admita alias en el apex.
2. Crea los registros:
   ```text
   @     CNAME/ALIAS   pardrops.pages.dev
   www   CNAME         pardrops.pages.dev
   ```
3. En Cloudflare → Workers & Pages → `pardrops` → Custom domains, añade
   `pardrops.qd.je` y `www.pardrops.qd.je` (ya están dados de alta en estado
   `initializing`, esperando DNS).
4. Espera a que Cloudflare emita el certificado (unos minutos tras propagar).

No cambies los nameservers de `qd.je` ni borres registros existentes: solo se
tocan los de la zona `pardrops.qd.je`.

---

## 8. Despliegue automático

`deploy.yml` se ejecuta en cada push a `main`:

1. `backend`: `./mvnw verify` (JDK 25) y `POST /v1/services/{id}/deploys` en Render.
2. `frontend`: `npm ci`, `npm run build:pages` con `vars.PARDROPS_API_URL` y
   `wrangler pages deploy`.

Secretos requeridos: `RENDER_API_KEY`, `RENDER_SERVICE_ID`,
`CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`. Variable:
`PARDROPS_API_URL`.

`ci.yml` compila y prueba en cada push y pull request. Si el build falla, el
despliegue no se ejecuta y la versión anterior sigue publicada (Render solo
promociona un deploy correcto).

---

## 9. Migración H2 → PostgreSQL

Procedimiento reproducible con `tools/MigrateH2ToPostgres.java`:

1. Copia de seguridad del fichero H2 (la herramienta la hace automáticamente en
   `backups/h2-<timestamp>/` y muestra el SHA-256).
2. Crea el esquema en PostgreSQL con la aplicación y la semilla desactivada:
   ```bash
   APP_SEED_ENABLED=false SPRING_PROFILES_ACTIVE=prod \
   SPRING_DATASOURCE_URL=... java -jar pardrops.jar
   ```
3. Ejecuta la migración:
   ```bash
   java -cp "h2.jar:postgresql.jar" tools/MigrateH2ToPostgres.java
   ```
   Inserta fila a fila con `ON CONFLICT DO NOTHING`, reintenta las tablas con
   claves foráneas y sincroniza las secuencias.
4. Compara recuentos (la propia herramienta los imprime) y arranca la aplicación
   con la semilla activada.

Las tablas de unión sin clave primaria (`product_categories`, `product_tags`,
`product_images`, `product_variants`) pueden recibir duplicados al reejecutar la
copia; se deduplicaron una vez y se añadieron índices únicos en
`product_categories` y `product_tags`.

---

## 10. Operación diaria

| Tarea | Cómo |
|---|---|
| Ver salud del backend | `curl https://pardrops-api.onrender.com/actuator/health` |
| Ver logs de Render | Panel → Logs, o `GET /v1/logs` con la API key |
| Ver despliegues | `GET /v1/services/{id}/deploys` |
| Revertir backend | Panel → Deploys → *Rollback* al deploy anterior |
| Revertir frontend | `wrangler pages deployments` y volver a publicar el build anterior |
| Ver estado de la BD | Consola de Neon → Monitoring |
| Cambiar URL de la API | Editar `PARDROPS_API_URL` y volver a desplegar el frontend |

### Seguridad

- Rota `APP_JWT_SECRET` solo con un despliegue coordinado: invalida las sesiones
  activas.
- Rota las claves de Render, Cloudflare, Neon y el PAT de GitHub si alguna se
  comparte por un canal no seguro.
- El administrador inicial creado por la semilla debe cambiar su contraseña; la
  cuenta heredada de H2 conserva la contraseña de desarrollo.
