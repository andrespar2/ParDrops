# Correo transaccional con Brevo

Documentación de la integración de correo de ParDrops: configuración SMTP,
eventos que envían correo, seguridad y verificación.

---

## 1. Arquitectura

```text
Evento de negocio (pedido, retiro, registro)
        │
        ├─► NotificationService ──► notificación interna (PostgreSQL)
        │
        └─► EmailService ──► MailGateway ──► SMTP Brevo (smtp-relay.brevo.com:587)
```

- `EmailService` centraliza el envío: HTML, reintentos, deduplicación y registro.
- `SmtpMailGateway` se activa con `app.mail.enabled=true`; sin él se usa el outbox
  local (`./data/mail-outbox`).
- `OutboxMailGateway` permite desarrollar sin credenciales ni red.
- Un fallo de correo **nunca** revierte la operación: se registra y la
  notificación interna se conserva.

## 2. Variables de entorno (Render)

| Variable | Valor |
|---|---|
| `MAIL_ENABLED` | `true` |
| `MAIL_HOST` | `smtp-relay.brevo.com` |
| `MAIL_PORT` | `587` |
| `MAIL_USERNAME` | `c12f3f001@smtp-brevo.com` |
| `MAIL_PASSWORD` | *secreto (SMTP key de Brevo)* |
| `MAIL_SMTP_AUTH` | `true` |
| `MAIL_STARTTLS` | `true` |
| `MAIL_FROM` | correo verificado en Brevo |
| `MAIL_FROM_NAME` | `ParDrops` |
| `MAIL_CONNECTION_TIMEOUT` | `10000` |
| `MAIL_READ_TIMEOUT` | `10000` |
| `MAIL_WRITE_TIMEOUT` | `10000` |

Equivalentes de Spring Boot aceptados: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`,
`SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`.

> Las credenciales viven **solo** en Render. No se versionan ni se registran en logs.

## 3. Eventos que envían correo

| Evento | Comprador | Vendedor | Administración |
|---|:---:|:---:|:---:|
| Registro de cuenta | ✓ | — | — |
| Nueva venta / pedido | ✓ | ✓ | ✓ |
| Confirmación de pago | ✓ | ✓ | — |
| Cambio de estado del pedido | ✓ | ✓ | — |
| Solicitud de retiro | — | ✓ | ✓ |
| Retiro en revisión / aprobado | — | ✓ | — |
| Retiro rechazado / completado | — | ✓ | ✓ (rechazo) |

Las plantillas usan la identidad de ParDrops, son responsivas y no incluyen
contraseñas, llaves Bre-B ni números completos de cuenta.

## 4. Reintentos y deduplicación

- Hasta **3 intentos** con espera progresiva (400 ms, 800 ms).
- Ventana de deduplicación de **10 minutos** por `destinatario + asunto + cuerpo`.
- Registro de cada intento con destinatario, asunto y resultado (sin secretos).
- Timeouts SMTP de 10 s para no bloquear la petición HTTP.

## 5. Prueba de envío

Endpoint (solo administradores):

```http
POST /api/admin/mail/test
Authorization: Bearer <token admin>
Content-Type: application/json

{ "to": "correo@dominio.com" }
```

Respuesta: `{ "sent": true|false, "to": "..." }`.

Desde consola:

```bash
TOKEN=$(curl -s -X POST https://pardrops-api.onrender.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@pardrops.com","password":"admin123"}' | jq -r .token)

curl -s -X POST https://pardrops-api.onrender.com/api/admin/mail/test \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"to":"tu-correo@dominio.com"}'
```

## 6. Estado de la verificación (2026-11-03)

**Bloqueado por Brevo, no por el código.** Brevo rechaza las conexiones desde
IPs no autorizadas:

```text
SMTP  → EAUTH 525  5.7.1 Unauthorized IP address
API   → 401 {"message":"We have detected you are using an unrecognised IP address …"}
```

- Render (plan gratuito) usa IPs de salida variables, por lo que no pueden
  añadirse de forma estable a la lista de IPs autorizadas.
- El endpoint de prueba responde correctamente `{"sent": false}` y el log
  registra los 3 intentos; la aplicación no falla ni revierte operaciones.

### Cómo desbloquearlo (acción en Brevo)

1. Entrar en **Brevo → Security → Authorised IPs**
   (<https://app.brevo.com/security/authorised_ips>) y **desactivar la
   restricción de IPs** (recomendado para Render) o autorizar la IP indicada
   en el error.
2. Si se desactiva la restricción, sirve tanto la **SMTP key** como la **API key**.
3. Repetir el `POST /api/admin/mail/test` y comprobar la recepción.

No se requieren cambios de código adicionales.

## 7. Seguridad

- Credenciales solo en Render; rotarlas si se comparten por un canal inseguro.
- No se registran contraseñas, llaves ni números de cuenta.
- Los correos solo se envían a los implicados y a administradores.
- El envío es *best effort*: la notificación interna es la fuente de verdad.

## 8. Diagnóstico rápido

| Síntoma | Causa probable |
|---|---|
| `sent: false` y `EAUTH 525` | IP no autorizada en Brevo |
| `sent: false` y `535` | Credenciales SMTP incorrectas |
| `sent: false` y timeout | Puerto bloqueado o SMTP caído |
| No llega pero `sent: true` | Revisar spam / remitente no verificado |
| Correos duplicados | No debería ocurrir (deduplicación de 10 min) |
