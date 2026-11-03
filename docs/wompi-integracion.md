# Integración de Wompi (estado y plan)

Documento de traspaso de la integración de pagos con Wompi Colombia.
**La pasarela todavía no está implementada**: aquí se registra lo configurado,
el diseño acordado y las tareas pendientes.

---

## 1. Estado actual

| Elemento | Estado |
|---|---|
| Project de GitHub | https://github.com/users/andrespar2/projects/9 |
| Issues | #108–#115 abiertos |
| Variables en Render | configuradas (sandbox activo, producción preparada) |
| Modelo de pago en pedidos | pendiente |
| Checkout y firma de integridad | pendiente |
| Webhook | pendiente |
| Pantalla de compra realizada | pendiente |
| Correos por rol | pendiente (el transporte Brevo ya funciona) |
| Pruebas sandbox | pendientes |
| Activación de producción | pendiente |

### Variables configuradas (solo nombres)

```text
WOMPI_ENV=sandbox
WOMPI_CURRENCY=COP
WOMPI_FIXED_FEE_COP=700
WOMPI_TEST_PUBLIC_KEY / WOMPI_TEST_PRIVATE_KEY
WOMPI_TEST_EVENTS_SECRET / WOMPI_TEST_INTEGRITY_SECRET
WOMPI_PROD_PUBLIC_KEY / WOMPI_PROD_PRIVATE_KEY
WOMPI_PROD_EVENTS_SECRET / WOMPI_PROD_INTEGRITY_SECRET
```

Las llaves privadas y los secretos viven **solo** en Render. La llave pública
se sirve al frontend por configuración de ejecución cuando se implemente el
checkout. Nunca se versionan ni se registran en logs.

## 2. Diseño acordado

```text
Checkout (Angular)
   │ POST /api/checkout/wompi  (carrito)
   ▼
Backend
   ├─ relee precios de la base de datos
   ├─ calcula subtotal + envío por tienda + cargo fijo 700
   ├─ crea pedido PAYMENT_PENDING y referencia única
   ├─ firma integridad SHA256 (reference + amount + currency + integrity secret)
   └─ devuelve publicKey, reference, amount, signature
   │
   ▼
Widget de Wompi
   │ pago
   ▼
Webhook  POST /api/webhooks/wompi
   ├─ valida checksum con events secret
   ├─ verifica reference, monto, moneda y ambiente
   ├─ consulta la transacción a Wompi (fuente de verdad)
   ├─ idempotencia por transaction.id
   ├─ APPROVED → pedido pagado + saldos retenidos + cargo 700 como ingreso
   └─ correos e internas (comprador, cada vendedor, administradores)
```

La pantalla de éxito solo se muestra cuando el backend confirma `APPROVED`.

## 3. Reglas de negocio

- Precios siempre desde la base de datos; el navegador no fija importes.
- El cargo fijo de **700 COP** se cobra una vez por checkout, se registra como
  ingreso de ParDrops y **no** se descuenta del vendedor.
- El envío se calcula por tienda (implementado previamente).
- No se añade IVA sobre el precio publicado.
- Cada vendedor recibe solo el importe de sus productos menos su comisión.
- Los saldos quedan retenidos hasta la entrega, como ya hace el sistema.
- Pedidos pendientes, rechazados o anulados no generan saldo.
- Webhook idempotente: un evento repetido no duplica pagos, saldos ni correos.

## 4. Pendientes por Issue

| Issue | Alcance |
|---|---|
| #108 | Campos `payment_reference`, `payment_status`, `wompi_transaction_id`, `service_fee` y `payment_method` en `orders` |
| #109 | Servicio de checkout: recálculo, referencia, firma SHA256 y endpoint |
| #110 | Webhook público con checksum dinámico, verificación remota e idempotencia |
| #111 | Aplicación del pago: estado, saldos por vendedor, ingreso de 700, auditoría |
| #112 | Pantalla de compra realizada con estados pendiente/rechazado y recarga segura |
| #113 | Correos diferenciados a comprador, cada vendedor y administradores |
| #114 | Notificaciones internas y panel de transacciones |
| #115 | Matriz de pruebas sandbox y cambio a producción |

## 5. Matriz de pruebas sandbox

1. Un producto, varios productos del mismo vendedor y multi-vendedor.
2. Cambio de precio antes de pagar (el backend manda).
3. Cargo de 700 una sola vez y su registro como ingreso.
4. Monto enviado a Wompi igual al total del backend.
5. Pago aprobado, rechazado y pendiente.
6. Evento duplicado y firma inválida (deben rechazarse).
7. Recarga de la pantalla de retorno sin duplicar operaciones.
8. Saldos: retenido por vendedor, sin acreditar el cargo de servicio.
9. Correos reales a comprador, vendedor y administrador (recepción verificada).
10. Notificaciones internas y ausencia de duplicados.

## 6. Seguridad

- Verificar siempre la transacción contra la API de Wompi; la URL de retorno no
  es evidencia de pago.
- Comparación de firmas en tiempo constante.
- No registrar tarjetas ni secretos.
- Un fallo de SMTP no revierte una compra aprobada.
- Auditar cada cambio de estado y cada movimiento de saldo.

## 7. Requisito externo

Wompi debe autorizar el modelo **marketplace** (cobro centralizado y dispersión
posterior a vendedores) antes de activar producción. Si no está habilitado, hay
que solicitarlo y documentar la respuesta antes de recibir dinero real.
