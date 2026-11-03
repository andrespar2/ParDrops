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
- **Transporte activo en producción: API HTTPS de Brevo** (`BrevoApiMailGateway`),
  porque Render bloquea el puerto 587 en instancias gratuitas.
- `SmtpMailGateway` queda disponible con `MAIL_TRANSPORT=smtp` (desarrollo o
  proveedores sin bloqueo de puertos).
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
| `MAIL_TRANSPORT` | `api` (HTTPS de Brevo; `smtp` como alternativa) |
| `BREVO_API_KEY` | *secreto (API key de Brevo)* |

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

**Correo operativo.** Se envió un mensaje real desde producción a
`jorgepardounu@gmail.com` con resultado:

```text
POST /api/admin/mail/test → {"sent": true}
Log: BrevoApiMailGateway "Brevo API aceptó el correo para jorgepardounu@gmail.com"
Log: EmailService "Correo enviado (intento 1)"
Brevo respondió 201 con messageId.
```

### Historial del bloqueo (resuelto)

- `smtp-relay.brevo.com:587` fallaba con `EAUTH 525 Unauthorized IP address`
  mientras la cuenta tenía restricción por IP.
- Tras habilitar los permisos, SMTP funcionó desde una IP autorizada, pero
  **Render bloquea el puerto 587** en instancias gratuitas (timeout).
- Solución definitiva: enviar por la **API HTTPS de Brevo** (puerto 443), que no
  sufre bloqueo de puertos. `MAIL_TRANSPORT=api` + `BREVO_API_KEY`.
- La API key también estaba sujeta a la restricción de IPs; quedó resuelta al
  habilitar los permisos en Brevo.

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

## 9. Remitente privado con el dominio propio

**Configurado y verificado.** El remitente de producción es
`no-reply@pardrops.qd.je` (nombre `ParDrops`); la cuenta personal ya no aparece.

- Dominio `pardrops.qd.je` autenticado en Brevo (DKIM ×2, código de verificación
  TXT y DMARC).
- Registros en la zona `pardrops.qd.je` de DigitalPlat:
  - `TXT @` → `brevo-code:…`
  - `CNAME brevo1._domainkey` → `b1.pardrops-qd-je.dkim.brevo.com.`
  - `CNAME brevo2._domainkey` → `b2.pardrops-qd-je.dkim.brevo.com.`
  - `TXT _dmarc` → `v=DMARC1; p=none; rua=mailto:rua@dmarc.brevo.com`
- Brevo respondió `Domain has been authenticated successfully`.
- Envío de prueba desde `no-reply@pardrops.qd.je` aceptado por Brevo (HTTP 201).

Variables en Render: `MAIL_FROM=no-reply@pardrops.qd.je`, `MAIL_FROM_NAME=ParDrops`.

`no-reply@` no necesita buzón: el dominio autenticado firma los envíos con DKIM
y las respuestas no se reciben (comportamiento esperado).
