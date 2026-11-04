# Integración de Wompi en ParDrops

Documento técnico de la integración de pagos con Wompi Colombia: flujo,
endpoints, reglas de negocio, correos, seguridad y activación de producción.

---

## 1. Estado

| Elemento | Estado |
|---|---|
| Checkout con Widget y firma SHA-256 | Implementado |
| Webhook `transaction.updated` con checksum dinámico | Implementado |
| Verificación remota de la transacción | Implementado |
| Pedido pendiente y confirmación solo con pago aprobado | Implementado |
| Saldos retenidos y distribución por vendedor | Implementado |
| Cargo fijo de servicio de 700 COP | Implementado |
| Pantalla de compra realizada | Implementado |
| Correos diferenciados (comprador, vendedor, administrador) | Implementado |
| Notificaciones internas | Implementado |
| Panel administrativo de transacciones | Implementado |
| Pruebas sandbox | Ver sección 9 |
| Producción | Activa; seguimiento externo en Issue #116 |

## 2. Variables de entorno

Nunca se versionan los valores. En Render se configuran en el servicio del
backend.

```env
WOMPI_ENV=sandbox            # o production
WOMPI_CURRENCY=COP
WOMPI_FIXED_FEE_COP=700

WOMPI_TEST_PUBLIC_KEY=
WOMPI_TEST_PRIVATE_KEY=
WOMPI_TEST_EVENTS_SECRET=
WOMPI_TEST_INTEGRITY_SECRET=

WOMPI_PROD_PUBLIC_KEY=
WOMPI_PROD_PRIVATE_KEY=
WOMPI_PROD_EVENTS_SECRET=
WOMPI_PROD_INTEGRITY_SECRET=
```

`WOMPI_ENV=production` usa exclusivamente las llaves `*_PROD_*`; `sandbox`
usa las `*_TEST_*`. No se mezclan.

En el repositorio solo aparecen los **nombres** de las variables
(`.env.example`, `render.yaml`). Las llaves privadas, el secreto de eventos y el
de integridad permanecen en el backend. La llave pública se entrega al
navegador únicamente dentro de la respuesta del checkout.

## 3. Flujo

```text
Checkout (Angular)
   │ POST /api/checkout  (carrito + datos del comprador)
   ▼
Backend
   ├─ relee precios y stock desde la base de datos
   ├─ agrupa por tienda, calcula envío y comisión existentes
   ├─ añade el cargo fijo de 700 (una sola vez por pedido)
   ├─ crea el pedido en PENDING / paymentStatus PENDING
   ├─ genera una referencia única PD-<pedido>-<aleatorio>
   └─ firma SHA256(reference + amount_in_cents + currency + integrity_secret)
   │
   ▼
Widget de Wompi (llave pública + referencia + monto + firma)
   │ pago
   ▼
Redirección del navegador a /compra/<referencia>?id=<transacción>
   │ (solo informativa)
   ▼
Webhook público POST /api/webhooks/wompi
   ├─ valida el checksum con el secreto de eventos
   ├─ comprueba el ambiente del evento
   ├─ consulta la transacción con la llave privada (fuente de verdad)
   ├─ valida referencia, monto y moneda
   ├─ aplica el pago de forma idempotente
   └─ después del commit envía correos y avisos
```

La pantalla de resultado consulta `GET /api/checkout/<referencia>` y solo
muestra «Compra realizada» cuando el backend confirma `APPROVED`. Si el
navegador llega con `?id=...`, ese identificador se usa solo como clave para
consultar la API de Wompi; nunca como prueba de pago.

## 4. Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/checkout` | Público | Crea el pedido pendiente y devuelve los datos firmados del widget. |
| `GET` | `/api/checkout/{reference}` | Público | Estado del pago. Acepta `?transactionId=` para verificar con Wompi. |
| `POST` | `/api/checkout/{reference}/retry` | Público | Nueva referencia para reintentar un pago rechazado (mismo pedido). |
| `POST` | `/api/webhooks/wompi` | Público, validado por checksum | Recibe `transaction.updated`. |
| `GET` | `/api/admin/payments` | ADMIN | Transacciones, comisiones y recaudo por servicio. |

URL de webhook en producción:
`https://pardrops-api.onrender.com/api/webhooks/wompi`

## 5. Reglas de negocio

- Los precios salen siempre de la base de datos; el navegador no los fija.
- Si el vendedor cambia un precio antes del pago, el siguiente checkout cobra
  el nuevo precio. La pantalla avisa cuando el total confirmado difiere del
  carrito local.
- El cargo fijo de **700 COP** se aplica una vez por pedido, aparece como
  concepto independiente y se registra en `platform_revenues` (tipo
  `SERVICE_FEE`). No se descuenta del neto del vendedor.
- El envío se calcula por tienda, como ya hacía el marketplace.
- La comisión existente de ParDrops se conserva por subpedido.
- El stock no se reserva al crear el checkout; se descuenta una sola vez cuando
  el pago queda aprobado.
- Los estados `DECLINED`, `VOIDED` y `ERROR` no acreditan saldos ni ingresos.
- Un pago aprobado no se degrada por eventos posteriores.
- Reintentar un pago reutiliza el pedido y el mismo cargo de servicio.

## 6. Distribución de ingresos

Para cada subpedido aprobado:

1. Se retiene el neto del vendedor (`sellerNetAmount`) como saldo pendiente.
2. La comisión queda registrada en el subpedido y en el saldo al liberarse.
3. El cargo de 700 se registra como ingreso de la plataforma, nunca del
   vendedor.
4. La liberación a saldo disponible sigue las reglas existentes (al entregar el
   pedido).

Los movimientos son idempotentes: el pedido solo pasa una vez a `APPROVED` y
`platform_revenues` tiene una restricción única por pedido y concepto.

## 7. Correos

Se envían después del commit, con reintentos y registro en
`payment_email_logs`:

| Rol | Plantilla | Contenido |
|---|---|---|
| Comprador | `BUYER_PURCHASE_CONFIRMED` | Confirmación, pedido, fecha, productos, subtotal, envío, cargo de 700, total, siguientes pasos y enlace. |
| Vendedor | `SELLER_SALE_CONFIRMED` | Solo sus productos, comisión, neto, estado del saldo, enlace a su panel e instrucciones. |
| Administración | `ADMIN_SALE_CONFIRMED` | Referencia y transacción, comprador, productos, vendedores, importes, comisiones y distribución. |

Un fallo de correo no revierte la compra. Cada combinación
pedido + tipo + destinatario se envía una sola vez; si falla, admite hasta tres
intentos.

## 8. Seguridad

- Checksum de eventos calculado con `signature.properties` en el orden recibido,
  más `timestamp` y el secreto de eventos, comparado en tiempo constante.
- Verificación remota con llave privada antes de acreditar un pago aprobado.
- Referencias únicas y consultas idempotentes.
- No se almacenan datos de tarjeta.
- Los secretos nunca se registran en logs ni se envían al frontend.
- El administrador no puede marcar como pagado un pedido de Wompi sin una
  transacción aprobada.
- Auditoría de cada pago aprobado y de cada estado no aprobado.

## 9. Matriz de pruebas sandbox

| # | Prueba | Resultado |
|---|---|---|
| 1 | Compra de un producto | Ver sección 9.1 |
| 2 | Varios productos del mismo vendedor | Cubierto por pruebas automatizadas |
| 3 | Productos de varios vendedores | Cubierto por pruebas automatizadas |
| 4 | Cambio de precio antes de pagar | Automatizado |
| 5 | Cargo de 700 una sola vez | Automatizado y sandbox real |
| 6 | Monto enviado a Wompi | Automatizado y sandbox real |
| 7 | Pago aprobado | Sandbox real |
| 8 | Pago rechazado | Sandbox real |
| 9 | Pago pendiente | Automatizado |
| 10 | Evento duplicado | Automatizado |
| 11 | Firma de webhook incorrecta | Automatizado |
| 12 | Recarga de la pantalla de resultado | Diseño idempotente + pruebas |
| 13 | Reintento de pago | Automatizado |
| 14 | Actualización de saldos | Automatizado y sandbox real |
| 15 | Pantalla de compra realizada | Implementada; verificación visual |
| 16 | Correo al comprador | Sandbox real (outbox) |
| 17 | Correo a cada vendedor | Sandbox real (outbox) |
| 18 | Correo al administrador | Sandbox real (outbox) |
| 19 | Notificaciones internas | Automatizado |
| 20 | Sin duplicados | Automatizado y evento repetido |

### 9.1 Resultados de la ejecución real en sandbox

Ejecutado contra `https://sandbox.wompi.co` con las llaves `*_test_*` y las
tarjetas de prueba de Wompi. No se usó dinero real.

| Prueba | Evidencia |
|---|---|
| Checkout de un producto | Pedido `PD-1-…`, subtotal 89.000, envío 25.900, cargo 700, total 115.600 |
| Monto enviado a Wompi | Transacción `12246077-1793748990-40309` por 11.560.000 centavos (`APPROVED`) |
| Pago aprobado | `paymentStatus=APPROVED`, pedido `PAID`, verificado con la API de Wompi |
| Distribución | Comisión 4.450; neto del vendedor 110.450 retenido como pendiente |
| Cargo de servicio | 700 registrado una sola vez como ingreso de ParDrops |
| Correos | Tres correos reales en el outbox: comprador, vendedor y administrador, con asuntos y contenidos distintos |
| Evento duplicado | Webhook con checksum válido respondió 200 y no duplicó correos ni saldos |
| Firma incorrecta | Webhook con checksum inválido respondió 400 y no confirmó el pedido |
| Pago rechazado | Tarjeta `4111…` → transacción `DECLINED`; sin saldos ni correos de compra |
| Reintento | Nueva referencia sobre el mismo pedido, mismo cargo de 700; pago aprobado con `4242…` |
| Widget | Abrió en modo pruebas con el monto 115.600 y procesó el pago; el backend confirmó la transacción |
| Pantalla éxito | Visible con pedido, referencia, fecha, productos, desglose, total y botones |
| Pantalla pendiente | «Estamos verificando tu pago» sin mostrar la compra como realizada |
| Pantalla rechazada | «El pago no fue aprobado» con opción de reintentar |

> Requisito técnico detectado: Wompi bloquea con HTTP 403 las URLs de
> redirección que no usan HTTPS. En producción `APP_FRONTEND_URL` debe ser
> `https://pardrops.qd.je` (ya configurado en Render).

## 10. Activación de producción

1. Llaves `WOMPI_PROD_*` cargadas en Render y verificadas contra el merchant real.
2. `WOMPI_ENV=production` configurado en Render.
3. Backend desplegado (`83b2643`), health `UP` y `/api/checkout` público.
4. Comprobado en producción: `POST /api/checkout` devuelve
   `environment: "prod"`, llave `pub_prod_…` y el total con el cargo de 700.
   El pedido de verificación `#74` se canceló después de la prueba.
5. Frontend desplegado en Cloudflare Pages; `/compra/:reference` responde y
   consulta el backend de producción.
6. SMTP/Brevo permanece configurado con `MAIL_TRANSPORT=api`.
7. **Pendiente externo**: registrar en el dashboard de Wompi la URL de eventos
   de producción `https://pardrops-api.onrender.com/api/webhooks/wompi`.
   Seguimiento en el Issue #116.

### Requisito externo

Antes de recibir dinero real, Wompi debe autorizar el modelo **marketplace**
(cobro centralizado en ParDrops y dispersión posterior a los vendedores). Si la
cuenta no tiene esa habilitación, hay que solicitarla y documentar la respuesta.
Seguimiento en el Issue #116.

