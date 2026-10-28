# Pruebas

## Resumen de ejecución

### Backend (Maven)

Comando utilizado:

```powershell
$env:JAVA_HOME = (Resolve-Path '.\.tools\jdk25').Path
cd pardrops
.\mvnw.cmd -o test
```

**Resultado de la última ejecución completa:**

- `Tests run: 215, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Frontend (Angular/Vitest)

Comando utilizado:

```bash
cd pardropsfrontend
npx ng test --watch=false
```

**Resultado:**

- `Test Files: 17 passed (17)`
- `Tests: 83 passed (83)`

### Compilación frontend

```bash
npm run build
```

**Resultado:** compilación exitosa con los warnings preexistentes de presupuesto SCSS.

## Pruebas de correo transaccional

| Clase o archivo | Casos relevantes |
|---|---|
| `EmailTemplateServiceTest` | Plantillas predeterminadas, actualización, restauración, 404, renderizado con escape y plantilla inactiva. |
| `EmailTemplateRendererTest` | Sustitución de variables, escape de HTML, asunto sin escape, fragmentos seguros y variables desconocidas. |
| `EmailServiceTest` | Entrega, absorción de fallos y destinatario vacío. |
| `SmtpMailGatewayTest` | Composición y envío con JavaMail, fallo envuelto. |
| `OutboxMailGatewayTest` | Archivos `.eml` y `.html`, contenido y nombres únicos. |
| `OrderEmailServiceTest` | Correo al vendedor, respaldo de correo de tienda, subpedido de plataforma, comprobante global y por parte, avisos de estado, IVA e importes. |
| `OrderServiceTest` | Envío del comprobante al pagar, aviso de estado y notificación interna. |
| `SellerOrderServiceTest` | Comprobante y aviso cuando el vendedor cambia su parte. |
| `AdminEmailTemplateIntegrationTest` | Listado, edición, restauración, vista previa y 403 para clientes. |
| `admin.service.spec.ts` | Endpoints de plantillas: listar, consultar, actualizar, restaurar y previsualizar. |
| `admin-email-templates.component.spec.ts` | Carga, selección, guardado, restauración y vista previa. |
| `admin.component.spec.ts` | Shell con once secciones, incluida `plantillas-correo`. |

## Pruebas del marketplace multivendedor

| Clase | Casos relevantes |
|---|---|
| `SellerDashboardServiceTest` | Resumen con aislamiento por vendedor, conteos correctos, ingresos y notificaciones recientes. |
| `SellerOrderServiceTest` | Listado solo de subpedidos propios, cambio de estado con validación de transiciones y notificación al cliente. |
| `SellerApplicationServiceTest` | Envío, validación de rol/estado, reintento tras rechazo, aprobación/rechazo y creación de tienda al aprobar. |
| `StoreServiceTest` | Edición de ficha propia, validación de nombre/slug, unicidad de slug y restricciones por propietario. |
| `OrderMultivendorIntegrationTest` | Creación de pedido con múltiples vendedores, subpedidos por tienda y distribución de importes. |
| `SecurityPermissionsTest` | Permisos: cliente no puede acceder a `/api/seller/**` (403), vendedor sí (200). |

## Verificación manual del correo

Con `MAIL_ENABLED=false` (modo outbox):

1. Crear un pedido con productos de una tienda.
2. Revisar en `./data/mail-outbox` el `.eml` y el `.html` del vendedor.
3. Entrar como administrador y cambiar el pedido a `PAID`.
4. Revisar el comprobante del comprador.
5. Cambiar a `PREPARED` o `SHIPPED` y revisar el aviso de estado.
6. Editar una plantilla en `/admin/plantillas-correo`, guardar y crear otro pedido para comprobar el texto nuevo.

## Notas

- Las pruebas no necesitan SMTP ni credenciales reales: usan el outbox en una carpeta temporal.
- Los tests de seguridad validan tanto el acceso (guards del frontend) como los permisos (reglas del backend).
- No existe una suite E2E con Playwright en el repositorio; el recorrido principal se verifica manualmente.
